import asyncio
import json
from uuid import UUID

import httpx
from fastapi import APIRouter, Depends, Request, status
from fastapi.responses import JSONResponse, StreamingResponse

from app.agents import chatbot, description, moderation, review_assistant, summarizer
from app.config import get_settings
from app.middleware.auth import require_authenticated, require_role
from app.models.requests import ChatRequest, ModerationRequest, ReviewAssistRequest
from app.models.responses import ApiResponse
from app import vector_store

router = APIRouter(prefix="/api/v1/ai", tags=["AI"])


@router.get("/health")
async def health(request: Request):
    settings = get_settings()
    checks: dict[str, str] = {}

    redis = getattr(request.app.state, "redis", None)
    if redis is None:
        checks["redis"] = "down"
    else:
        try:
            await redis.ping()
            checks["redis"] = "ok"
        except Exception:
            checks["redis"] = "down"

    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            r = await client.get(f"{settings.ollama_url}/api/tags")
            checks["ollama"] = "ok" if r.status_code == 200 else "degraded"
    except Exception:
        checks["ollama"] = "down"

    checks["vector_store"] = "ok" if vector_store._store is not None else "down"

    all_ok = all(v == "ok" for v in checks.values())
    return JSONResponse(
        status_code=200 if all_ok else 503,
        content={"status": "ok" if all_ok else "degraded", "checks": checks},
    )


@router.post("/chat")
async def chat_endpoint(
    request: ChatRequest,
    caller_user_id: UUID = Depends(require_authenticated),
):
    response = await chatbot.chat(
        message=request.message,
        history=request.history,
        place_ids=request.placeIds,
        caller_user_id=caller_user_id,
    )
    return ApiResponse.ok(response.model_dump())


@router.post("/chat/stream")
async def chat_stream_endpoint(
    request: ChatRequest,
    caller_user_id: UUID = Depends(require_authenticated),
):
    """Server-Sent Events stream. Each event is `data: <json>\n\n`.
    Token events: {"token": "..."}
    Final event:  {"done": true, "suggestedPlaceIds": [...]}
    """
    async def _sse():
        async for event in chatbot.stream_chat(
            message=request.message,
            history=request.history,
            place_ids=request.placeIds,
            caller_user_id=caller_user_id,
        ):
            yield f"data: {json.dumps(event)}\n\n"

    return StreamingResponse(_sse(), media_type="text/event-stream")


@router.post("/review-assist")
async def review_assist_endpoint(
    request: ReviewAssistRequest,
    caller_user_id: UUID = Depends(require_authenticated),
):
    prompt = await review_assistant.get_writing_prompt(
        place_id=request.placeId,
        score=request.score,
        selected_tags=request.selectedTags,
        partial_body=request.partialBody,
    )
    return ApiResponse.ok(prompt)


@router.post("/places/{place_id}/summary")
async def generate_summary_endpoint(place_id: UUID):
    result = await summarizer.generate_and_store(place_id)
    if result is None:
        return ApiResponse.error("Not enough reviews to generate a summary yet")
    return ApiResponse.ok(result, "Summary generated")


@router.post("/moderate", status_code=status.HTTP_202_ACCEPTED)
async def moderate_endpoint(
    request: ModerationRequest,
    _role: str = Depends(require_role("ADMIN", "MODERATOR")),
):
    asyncio.create_task(moderation.moderate(
        rating_id=request.ratingId,
        place_id=request.placeId,
        user_id=request.userId,
        score=request.score,
        title=request.title,
        body=request.body,
        tags=request.tags,
    ))
    return ApiResponse.ok(None, "Moderation queued")


@router.post("/places/{place_id}/description")
async def generate_description_endpoint(
    place_id: UUID,
    _role: str = Depends(require_role("ADMIN")),
):
    desc = await description.generate_on_demand(place_id)
    return ApiResponse.ok(desc, "Description generated")
