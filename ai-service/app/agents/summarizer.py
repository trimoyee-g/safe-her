"""Agent 2: Safety Narrative Summarizer

Triggered on every Nth new review (Redis counter).
Reads up to 50 recent reviews, synthesises a 2-3 sentence plain-language
safety summary, then broadcasts it via Kafka so place-service can update.
"""
import logging
from uuid import UUID

import redis.asyncio as aioredis
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from app.config import get_settings
from app.clients import place_client, rating_client
from app.kafka import producer
from app.models.events import PlaceSummaryUpdatedEvent

logger = logging.getLogger(__name__)

_COUNTER_KEY = "ai:summary:counter:"

_SYSTEM = """You are a safety analyst for safeher, a platform helping people —
especially women — assess the safety of public places.

Given a collection of user reviews about a specific place, write a concise
2–3 sentence safety summary that:
1. States the overall safety impression neutrally and factually
2. Highlights the most commonly mentioned positive safety factors
3. Notes any significant safety concerns if present

Be specific. Output the summary text only. No preamble. Maximum 80 words."""

_redis: aioredis.Redis | None = None


def set_redis(client: aioredis.Redis) -> None:
    global _redis
    _redis = client


async def maybe_regenerate(place_id: UUID, total_ratings: int) -> None:
    settings = get_settings()
    if total_ratings < settings.summarizer_min_reviews:
        return

    key = _COUNTER_KEY + str(place_id)
    count = await _redis.incr(key)
    await _redis.expire(key, 30 * 24 * 3600)

    if count % settings.summarizer_regenerate_every != 0 and count != 1:
        return

    await generate_and_store(place_id)


async def generate_and_store(place_id: UUID) -> str | None:
    settings = get_settings()
    try:
        ratings_data = await rating_client.get_ratings_by_place(place_id, "NEWEST", 0, 50)
        reviews = ratings_data.get("content", [])

        if len(reviews) < settings.summarizer_min_reviews:
            return None

        place = await place_client.get_place(place_id)
        reviews_text = _format_reviews(reviews)

        user_message = (
            f"Place: {place.get('name')} ({place.get('category')}) "
            f"in {place.get('city', 'unknown city')}, {place.get('country', 'unknown country')}\n"
            f"Overall safety score: {place.get('safetyScore', 0):.1f}/5.0 based on {len(reviews)} reviews\n\n"
            f"Reviews:\n{reviews_text}"
        )

        llm = ChatOllama(
            model=settings.ollama_model_summarization,
            base_url=settings.ollama_url,
            num_predict=200,
            temperature=0.1,
        )
        chain = ChatPromptTemplate.from_messages([
            ("system", _SYSTEM),
            ("human", "{user_message}"),
        ]) | llm

        result = await chain.ainvoke({"user_message": user_message})
        summary = result.content.strip()

        event = PlaceSummaryUpdatedEvent(
            placeId=place_id,
            summary=summary,
            reviewsAnalysed=len(reviews),
        )
        await producer.send(settings.kafka_topic_place_summary_updated, str(place_id), event)
        logger.info(f"Generated summary for place [{place_id}] ({len(reviews)} reviews)")
        return summary

    except Exception as e:
        logger.error(f"Summary generation failed for place [{place_id}]: {e}")
        return None


def _format_reviews(reviews: list) -> str:
    parts = []
    for r in reviews:
        body = (r.get("body") or "").strip()
        if not body:
            continue
        tags = r.get("tags") or []
        tag_str = f" [tags: {', '.join(tags)}]" if tags else ""
        snippet = body[:300] + "…" if len(body) > 300 else body
        parts.append(f"Score {r.get('score', 0)}/5{tag_str}: {snippet}")
        if len(parts) >= 30:
            break
    return "\n---\n".join(parts)
