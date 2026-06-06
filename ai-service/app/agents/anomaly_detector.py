"""Agent 4: Rating Anomaly Detector

Two-stage detection:
  Stage 1 (Redis): velocity counter per place in a rolling window.
  Stage 2 (LLM):  if velocity exceeds threshold, fetch the batch and ask
                  the LLM whether the pattern looks coordinated.
  Confirmed coordination → flag all suspicious reviews via Kafka.
"""
import logging
from typing import List
from uuid import UUID

import redis.asyncio as aioredis
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field

from app.config import get_settings
from app.clients import rating_client
from app.kafka import producer
from app.models.events import ReviewFlaggedEvent

logger = logging.getLogger(__name__)

_VELOCITY_KEY = "ai:velocity:"

_SYSTEM = """You are a fraud detection AI for safeher, a women's safety platform.

Analyse this batch of recent reviews for a single place flagged for high submission velocity.
Determine whether the batch shows signs of coordinated manipulation:
- Multiple reviews with near-identical language
- All reviews giving the same extreme score (all 5s or all 1s)
- Score contradicts review sentiment
- Reviews lack specific local knowledge

{format_instructions}

Be conservative — only flag when clearly evident. False positives harm genuine users."""

_redis: aioredis.Redis | None = None


def set_redis(client: aioredis.Redis) -> None:
    global _redis
    _redis = client


class _AnomalyResult(BaseModel):
    coordinated: bool
    confidence: float = Field(ge=0.0, le=1.0)
    suspicious_ids: List[str] = Field(default_factory=list)
    reason: str


async def check_velocity(rating_id: str, place_id: UUID) -> None:
    settings = get_settings()
    key = _VELOCITY_KEY + str(place_id)

    count = await _redis.incr(key)
    if count == 1:
        await _redis.expire(key, settings.anomaly_velocity_window_minutes * 60)

    logger.debug(f"Velocity [{place_id}] = {count}/{settings.anomaly_velocity_threshold}")

    if count >= settings.anomaly_velocity_threshold and count % settings.anomaly_velocity_threshold == 0:
        logger.warning(f"High velocity [{place_id}]: {count} ratings – triggering anomaly check")
        await _analyze(place_id)


async def _analyze(place_id: UUID) -> None:
    settings = get_settings()
    try:
        ratings_data = await rating_client.get_ratings_by_place(place_id, "NEWEST", 0, 30)
        reviews = ratings_data.get("content", [])
        if not reviews:
            return

        batch_text = "\n".join(
            f'[id:{r.get("id")}] score={r.get("score")} tags={r.get("tags", [])} '
            f'body="{(r.get("body") or "")[:200]}"'
            for r in reviews
        )

        parser = PydanticOutputParser(pydantic_object=_AnomalyResult)
        prompt = ChatPromptTemplate.from_messages([
            ("system", _SYSTEM),
            ("human", "Recent reviews for place {place_id}:\n{batch_text}"),
        ]).partial(format_instructions=parser.get_format_instructions())

        llm = ChatOllama(
            model=settings.ollama_model_anomaly,
            base_url=settings.ollama_url,
            num_predict=400,
            temperature=0.1,
        )
        chain = prompt | llm | parser
        result: _AnomalyResult = await chain.ainvoke({
            "place_id": str(place_id),
            "batch_text": batch_text,
        })

        if not result.coordinated or result.confidence < 0.70:
            logger.debug(f"Anomaly [{place_id}]: NOT coordinated ({result.confidence:.2f})")
            return

        logger.warning(f"COORDINATED RATINGS [{place_id}] confidence={result.confidence:.2f}")
        for sid in result.suspicious_ids:
            event = ReviewFlaggedEvent(
                ratingId=sid,
                placeId=place_id,
                reason="COORDINATED",
                confidence=result.confidence,
                autoSuppressed=False,
            )
            await producer.send(settings.kafka_topic_review_flagged, sid, event)

    except Exception as e:
        logger.error(f"Anomaly detection failed for place [{place_id}]: {e}")
