"""Agent 5: Safety Chatbot — multi-agent agentic RAG.

Three agents cooperate on every turn:

  1. Retrieval agent   — pulls first-party review/place data we already have
                          (pgvector similarity search + place/rating services).
  2. Web research agent — searches the open web for whatever public safety
                          signal exists (agents/web_research.py). This is the
                          cold-start bridge: it gives a useful answer on day
                          one, before our own review corpus has any density.
  3. Synthesis agent    — combines both, cites every claim back to a source,
                          and reports a *programmatic* confidence level (not
                          just the model's self-report) so the UI can show
                          "based on 3 reviews" vs "no data found, general
                          guidance only" instead of a flat, unqualified verdict.

State machine (LangGraph):
  START
    ↓
  generate  ─── calls Ollama with first-party + web context, conversation history
    ↓
  grade     ─── lightweight grader checks if response is grounded in context
    ↓ GROUNDED or max retries reached
  END
    ↓ NOT_GROUNDED (up to MAX_RETRIES times)
  (back to generate)

The grader uses phi3:mini (fast) for near-zero overhead. If confidence is
"no_data" (nothing specific found for either agent), the grader always passes
— general advice needs no grounding.

As our own review corpus grows, `_compute_confidence` shifts weight toward
first-party data automatically (see `chatbot_first_party_confidence_floor` in
config.py) — the web research agent stays as a permanent supplement, not a
permanent crutch.
"""
import asyncio
import hashlib
import json
import logging
from functools import lru_cache
from typing import AsyncGenerator, List, Optional, TypedDict
from uuid import UUID

import redis.asyncio as aioredis
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_ollama import ChatOllama
from langgraph.graph import END, START, StateGraph

from app.config import get_settings
from app.clients import place_client, rating_client
from app.models.requests import ChatMessage
from app.models.responses import ChatResponse, Source
from app import vector_store
from app.agents import web_research

logger = logging.getLogger(__name__)

MAX_HISTORY_TURNS = 10
MAX_RETRIES = 2
_CONTEXT_CACHE_TTL = 120  # seconds — short enough to reflect new reviews promptly

_redis: aioredis.Redis | None = None


def set_redis(client: aioredis.Redis) -> None:
    global _redis
    _redis = client


def _context_cache_key(place_ids: List[UUID], message: str) -> str:
    # Web results depend on the question, not just the place — key on both.
    ids = sorted(str(pid) for pid in place_ids)
    payload = json.dumps({"ids": ids, "q": message})
    return f"ai:chat:ctx:{hashlib.sha256(payload.encode()).hexdigest()}"


@lru_cache(maxsize=1)
def _get_main_llm() -> ChatOllama:
    settings = get_settings()
    return ChatOllama(
        model=settings.ollama_model_chatbot,
        base_url=settings.ollama_url,
        num_predict=512,
        temperature=0.1,
    )


@lru_cache(maxsize=1)
def _get_grader_llm() -> ChatOllama:
    settings = get_settings()
    return ChatOllama(
        model=settings.ollama_model_moderation,
        base_url=settings.ollama_url,
        num_predict=5,
        temperature=0.0,
    )

_SYSTEM_PROMPT = """You are SafeGuide, a friendly and knowledgeable safety assistant on SafeHer –
a platform helping people, especially women, make informed decisions about
the safety of public places.

You are given two kinds of information below, and they are NOT equally trustworthy:
  - First-party review data: reviews submitted by our own users. Specific and attributable.
  - Web research: news articles, forums, other review sites. Useful when we don't have
    our own reviews yet, but unverified and must be attributed, never stated as settled fact.

Rules:
1. Prefer first-party review data. Quote specific concerns reviewers mentioned.
2. When you use a web result, attribute it explicitly (e.g. "a recent news article
   mentions...") — never present it as a confirmed fact.
3. Always be explicit about how much data backs your answer. Say things like
   "based on 4 reviews on our platform" or "we don't have reviews for this place yet,
   but web sources mention..." or "we couldn't find specific information about this
   place — here's general guidance instead." Never give a flat verdict with no
   indication of how confident it is.
4. Never invent specifics (lighting, foot traffic, incidents) that aren't in the
   context below. If nothing addresses the user's specific question, say so plainly
   rather than guessing.
5. Give practical, actionable advice where the data supports it (safer times, what
   to be aware of, which exit/entrance to use).
6. Never downplay genuine safety concerns to seem positive.
7. Keep responses concise — 2–4 short paragraphs maximum.
8. For emergencies, always direct users to call 100 (India police) or 112 (emergency).

{context}"""

_GRADER_PROMPT = """Does this response stay grounded in the provided context, and does it
correctly attribute web-sourced claims as unverified rather than stating them as fact?
Answer GROUNDED if:
  - the response gives general safety advice (no context needed), OR
  - the specific claims are supported by the context below and web claims are attributed.
Answer NOT_GROUNDED only if the response makes specific factual claims clearly absent
from the context, or states a web-sourced claim as settled fact.

Context (excerpt):
{context}

Response:
{response}

Reply with exactly one word: GROUNDED or NOT_GROUNDED"""


# ── LangGraph state ───────────────────────────────────────────────────────────

class _State(TypedDict):
    messages: List[dict]   # list of {role, content}
    place_context: str     # combined first-party + web context, pre-built
    confidence: str
    generation: str
    retries: int


# ── Graph nodes ───────────────────────────────────────────────────────────────

async def _generate(state: _State) -> _State:
    system_text = _SYSTEM_PROMPT.format(context=state["place_context"])

    lc_messages = [SystemMessage(content=system_text)]
    for m in state["messages"]:
        if m["role"] == "user":
            lc_messages.append(HumanMessage(content=m["content"]))
        else:
            lc_messages.append(AIMessage(content=m["content"]))

    try:
        response = await _get_main_llm().ainvoke(lc_messages)
        return {**state, "generation": response.content, "retries": state.get("retries", 0) + 1}
    except Exception as e:
        logger.error(f"Chatbot generation failed: {e}")
        return {**state, "generation": "I'm temporarily unavailable. Please try again.", "retries": 99}


async def _grade(state: _State) -> str:
    """Conditional edge: returns 'end' or 'retry'."""
    if state.get("retries", 0) >= MAX_RETRIES:
        return "end"

    # Nothing specific found for either agent → general advice needs no grounding check
    if state["confidence"] == "no_data":
        return "end"

    prompt_text = _GRADER_PROMPT.format(
        context=state["place_context"][:1000],
        response=state["generation"][:400],
    )
    try:
        result = await _get_grader_llm().ainvoke([HumanMessage(content=prompt_text)])
        if "NOT_GROUNDED" in result.content.upper():
            logger.debug("Response graded NOT_GROUNDED – retrying generation")
            return "retry"
    except Exception as e:
        logger.warning(f"Grader failed (defaulting to GROUNDED): {e}")

    return "end"


# ── Build graph (once at module load) ─────────────────────────────────────────

def _build_graph():
    g = StateGraph(_State)
    g.add_node("generate", _generate)
    g.add_edge(START, "generate")
    g.add_conditional_edges("generate", _grade, {"end": END, "retry": "generate"})
    return g.compile()


_graph = _build_graph()


# ── Public API ────────────────────────────────────────────────────────────────

async def chat(
    message: str,
    history: Optional[List[ChatMessage]],
    place_ids: Optional[List[UUID]],
    caller_user_id: Optional[UUID],
) -> ChatResponse:
    if not place_ids:
        place_ids = await vector_store.search_places(message)

    gathered = await _gather_context(place_ids, message)

    history = history or []
    trimmed = history[-(MAX_HISTORY_TURNS * 2):]
    messages = [{"role": m.role.lower(), "content": m.content} for m in trimmed]
    messages.append({"role": "user", "content": message})

    initial: _State = {
        "messages": messages,
        "place_context": gathered["context"],
        "confidence": gathered["confidence"],
        "generation": "",
        "retries": 0,
    }

    final = await _graph.ainvoke(initial)

    response = ChatResponse(
        message=final["generation"].strip(),
        suggestedPlaceIds=place_ids or [],
        sources=gathered["sources"],
        confidence=gathered["confidence"],
    )
    logger.debug(
        "Chatbot response for user [%s]: %d chars, confidence=%s, %d sources",
        caller_user_id, len(response.message), response.confidence, len(response.sources),
    )
    return response


async def stream_chat(
    message: str,
    history: Optional[List[ChatMessage]],
    place_ids: Optional[List[UUID]],
    caller_user_id: Optional[UUID],
) -> AsyncGenerator[dict, None]:
    """Stream tokens directly without a grading loop.

    Streaming and retroactive grading are incompatible — once tokens are sent,
    they can't be recalled. The graded non-streaming `chat()` remains available
    when groundedness matters more than latency. Retrieval + web research still
    run up front (unavoidable — we need the context before we can generate),
    only the generation itself streams.
    """
    if not place_ids:
        place_ids = await vector_store.search_places(message)

    gathered = await _gather_context(place_ids, message)

    history = history or []
    trimmed = history[-(MAX_HISTORY_TURNS * 2):]

    system_text = _SYSTEM_PROMPT.format(context=gathered["context"])
    lc_messages: list = [SystemMessage(content=system_text)]
    for m in trimmed:
        if m.role.lower() == "user":
            lc_messages.append(HumanMessage(content=m.content))
        else:
            lc_messages.append(AIMessage(content=m.content))
    lc_messages.append(HumanMessage(content=message))

    try:
        async for chunk in _get_main_llm().astream(lc_messages):
            if chunk.content:
                yield {"token": chunk.content}
    except Exception as e:
        logger.error(f"Chatbot stream failed: {e}")
        yield {"token": "I'm temporarily unavailable. Please try again."}

    yield {
        "done": True,
        "suggestedPlaceIds": [str(pid) for pid in (place_ids or [])],
        "confidence": gathered["confidence"],
        "sources": [s.model_dump(mode="json") for s in gathered["sources"]],
    }


# ── Context gathering (retrieval agent + web research agent) ──────────────────

async def _fetch_place_data(place_id: UUID) -> dict:
    """Raw place + review data for one place — feeds both the first-party text
    block and, for the first referenced place, the web research query."""
    try:
        place, ratings_data = await asyncio.gather(
            place_client.get_place(place_id),
            rating_client.get_ratings_by_place(place_id, "NEWEST", 0, 20),
        )
        return {"place_id": place_id, "place": place, "reviews": ratings_data.get("content", [])}
    except Exception as e:
        logger.warning(f"Could not fetch data for place [{place_id}]: {e}")
        return {"place_id": place_id, "place": {}, "reviews": []}


def _build_first_party_text(place_payloads: list[dict]) -> tuple[str, int, list[Source]]:
    if not place_payloads:
        return "No specific places referenced yet.", 0, []

    sections: list[str] = []
    review_sources: list[Source] = []
    total_reviews = 0

    for payload in place_payloads:
        place = payload["place"]
        reviews = payload["reviews"]
        place_id = payload["place_id"]
        name = place.get("name", "Unknown")
        category = place.get("category", "OTHER")
        city = place.get("city") or place.get("country") or "Unknown"
        safety_score = place.get("safetyScore") or 0.0
        total_ratings = place.get("totalRatings") or 0

        lines = [
            f"\n## {name} ({category})",
            f"Location: {city}",
            f"Safety score: {safety_score:.1f}/5 based on {total_ratings} reviews",
            "",
        ]

        snippets = []
        reviewed_count = 0
        for r in reviews:
            body = (r.get("body") or "").strip()
            if not body:
                continue
            reviewed_count += 1
            if len(snippets) >= 10:
                continue
            tags = r.get("tags") or []
            tag_str = f"[{', '.join(tags)}] " if tags else ""
            snippet = body[:250] + "…" if len(body) > 250 else body
            snippets.append(f"- Score {r.get('score')}/5 {tag_str}: {snippet}")

        lines.append("Recent reviews:\n" + "\n".join(snippets) if snippets
                      else "No detailed review text available yet.")
        sections.append("\n".join(lines))

        total_reviews += reviewed_count
        if reviewed_count:
            review_sources.append(Source(
                kind="review",
                label=f"{reviewed_count} review(s) of {name}",
                placeId=place_id,
            ))

    return "\n".join(sections), total_reviews, review_sources


def _compute_confidence(review_count: int, web_count: int) -> str:
    settings = get_settings()
    if review_count >= settings.chatbot_first_party_confidence_floor:
        return "high"
    if review_count > 0 or web_count >= 2:
        return "medium"
    if web_count >= 1:
        return "low"
    return "no_data"


async def _gather_context(place_ids: Optional[List[UUID]], message: str) -> dict:
    """Runs the retrieval agent and web research agent, then merges their
    output into one prompt-ready context block plus structured sources and a
    programmatic confidence label (not just the LLM's self-report)."""
    if _redis is not None:
        key = _context_cache_key(place_ids or [], message)
        try:
            cached = await _redis.get(key)
            if cached:
                data = json.loads(cached)
                data["sources"] = [Source(**s) for s in data["sources"]]
                logger.debug("Chat context cache hit")
                return data
        except Exception as e:
            logger.warning("Context cache read failed: %s", e)

    place_payloads = []
    if place_ids:
        place_payloads = await asyncio.gather(*[_fetch_place_data(pid) for pid in place_ids])

    first_party_text, review_count, review_sources = _build_first_party_text(place_payloads)

    primary_place = place_payloads[0]["place"] if place_payloads else {}
    web_sources = await web_research.search(
        place_name=primary_place.get("name"),
        city=primary_place.get("city"),
        user_message=message,
    )

    context_parts = [f"### First-party review data (our platform)\n{first_party_text}"]
    sources = list(review_sources)

    if web_sources:
        web_lines = []
        for i, ws in enumerate(web_sources, start=1):
            web_lines.append(f"[W{i}] {ws.title}: {ws.snippet}\n    (source: {ws.url})")
            sources.append(Source(kind="web", label=ws.title, url=ws.url))
        context_parts.append(
            "### Web research (external, unverified — attribute explicitly, do not "
            "state as fact)\n" + "\n".join(web_lines)
        )
    else:
        context_parts.append("### Web research\nNo relevant web results found.")

    confidence = _compute_confidence(review_count, len(web_sources))
    result = {
        "context": "\n\n".join(context_parts),
        "sources": sources,
        "confidence": confidence,
    }

    if _redis is not None:
        try:
            cacheable = {**result, "sources": [s.model_dump(mode="json") for s in sources]}
            await _redis.setex(key, _CONTEXT_CACHE_TTL, json.dumps(cacheable))
        except Exception as e:
            logger.warning("Context cache write failed: %s", e)

    return result
