"""Agent 6: Place Description Generator

Triggered on place.created Kafka events, or on-demand from the admin panel.
Generates a 2-sentence factual description of the place and broadcasts it
via Kafka so place-service can persist it.
"""
import logging
from functools import lru_cache
from uuid import UUID

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from app.config import get_settings
from app.clients import place_client
from app.kafka import producer
from app.models.events import PlaceDescriptionUpdatedEvent

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def _get_llm() -> ChatOllama:
    settings = get_settings()
    return ChatOllama(
        model=settings.ollama_model_description,
        base_url=settings.ollama_url,
        num_predict=80,
        temperature=0.2,
    )


_SYSTEM = """You are a local knowledge assistant for safeher, a safety-rating platform.

Given a place's name, category, and location, write a factual, neutral 2-sentence description
that helps users understand what kind of place it is and its general context.

Focus on facts relevant to safety assessment (e.g. "busy commercial area", "major transit hub").
Do NOT make safety judgements. Output the description text only. No preamble. Maximum 40 words."""


async def generate_for_new_place(place_id: UUID) -> None:
    try:
        place = await place_client.get_place(place_id)
        desc = await _generate(place)
        await _publish(place_id, desc)
        logger.info(f"Generated description for new place [{place_id}]")
    except Exception as e:
        logger.warning(f"Description generation failed for place [{place_id}]: {e}")


async def generate_on_demand(place_id: UUID) -> str:
    place = await place_client.get_place(place_id)
    desc = await _generate(place)
    await _publish(place_id, desc)
    logger.info(f"Generated description on-demand for place [{place_id}]")
    return desc


async def _generate(place: dict) -> str:
    user_message = (
        f"Place name: {place.get('name')}\n"
        f"Category: {place.get('category', 'OTHER')}\n"
        f"City: {place.get('city', 'unknown')}\n"
        f"Country: {place.get('country', 'unknown')}"
    )
    chain = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM),
        ("human", "{user_message}"),
    ]) | _get_llm()
    result = await chain.ainvoke({"user_message": user_message})
    return result.content.strip()


async def _publish(place_id: UUID, description: str) -> None:
    settings = get_settings()
    event = PlaceDescriptionUpdatedEvent(placeId=place_id, description=description)
    await producer.send(settings.kafka_topic_place_description_updated, str(place_id), event)
