"""Agent 1: Review Moderation

Classifies review content as CLEAN / SPAM / ABUSE / FAKE / COORDINATED.
Uses LangChain's PydanticOutputParser for reliable structured output.
Above auto-suppress-threshold → suppress + publish ai.review.flagged.
Above flag-threshold         → publish ai.review.flagged for human review.
"""
import logging
from functools import lru_cache
from typing import List, Optional
from uuid import UUID

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field

from app.config import get_settings
from app.clients import rating_client
from app.kafka import producer
from app.models.events import ReviewFlaggedEvent

logger = logging.getLogger(__name__)

_SYSTEM = """You are a content moderation AI for safeher, a safety-rating platform where women
and marginalised communities share experiences about public places.

Classify the review as one of:
- CLEAN: genuine, relevant, appropriate review
- SPAM: low effort, irrelevant, or promotional content
- ABUSE: harassment, hate speech, threats, or discriminatory language
- FAKE: score clearly does not match the review sentiment; implausible claims
- COORDINATED: appears to be part of a brigading attempt

Discussing safety concerns — street harassment, poor lighting, feeling unsafe — is ALWAYS CLEAN.

{format_instructions}"""


@lru_cache(maxsize=1)
def _get_llm() -> ChatOllama:
    settings = get_settings()
    return ChatOllama(
        model=settings.ollama_model_moderation,
        base_url=settings.ollama_url,
        num_predict=256,
        temperature=0.1,
    )


class _ModerationResult(BaseModel):
    classification: str = Field(description="One of: CLEAN, SPAM, ABUSE, FAKE, COORDINATED")
    confidence: float = Field(ge=0.0, le=1.0)
    reason: str = Field(description="One sentence explanation")


async def moderate(
    rating_id: str,
    place_id: UUID,
    user_id: Optional[UUID],
    score: int,
    title: Optional[str],
    body: Optional[str],
    tags: Optional[List[str]],
) -> None:
    if not (body or title):
        # Called from Kafka: event carries no text, so fetch it from rating-service.
        rating = await rating_client.get_rating(rating_id)
        body = (rating.get("body") or "").strip() or None
        title = (rating.get("title") or "").strip() or None
        if tags is None:
            tags = rating.get("tags")
        if not (body or title):
            logger.debug(f"Review [{rating_id}] has no text – skipping moderation")
            return

    settings = get_settings()  # needed for thresholds below
    parser = PydanticOutputParser(pydantic_object=_ModerationResult)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM),
        ("human", "{review_text}"),
    ]).partial(format_instructions=parser.get_format_instructions())

    chain = prompt | _get_llm() | parser

    review_text = _build_review_text(score, title, body, tags)
    result: _ModerationResult | None = None
    for attempt in range(2):
        try:
            result = await chain.ainvoke({"review_text": review_text})
            break
        except Exception as e:
            if attempt == 0:
                logger.warning(f"Moderation parse failed (retrying) [{rating_id}]: {e}")
            else:
                logger.error(f"Moderation failed after 2 attempts [{rating_id}]: {e}")

    if result is None:
        return

    logger.info(f"Moderation [{rating_id}] → {result.classification} ({result.confidence:.2f})")

    if result.classification == "CLEAN":
        return

    auto_suppressed = result.confidence >= settings.moderation_auto_suppress_threshold
    if result.confidence >= settings.moderation_flag_threshold:
        if auto_suppressed:
            try:
                await rating_client.suppress_rating(rating_id)
                logger.warning(f"Auto-suppressed [{rating_id}] reason={result.reason}")
            except Exception as e:
                logger.error(f"Failed to suppress rating [{rating_id}]: {e}")

        event = ReviewFlaggedEvent(
            ratingId=rating_id,
            placeId=place_id,
            userId=user_id,
            reason=result.classification,
            confidence=result.confidence,
            autoSuppressed=auto_suppressed,
        )
        await producer.send(settings.kafka_topic_review_flagged, rating_id, event)


def _build_review_text(score, title, body, tags) -> str:
    parts = [f"Safety score given: {score}/5"]
    if title:
        parts.append(f"Title: {title}")
    if body:
        parts.append(f"Review: {body}")
    if tags:
        parts.append(f"Tags: {', '.join(tags)}")
    return "\n".join(parts)
