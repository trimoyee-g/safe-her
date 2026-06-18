"""Agent 3: Smart Review Assistant

Synchronous, low-latency call (<1s target).
Given the user's partial review state (score + tags), returns a 1-sentence
writing prompt to help them write a more useful safety review.
"""
import logging
from functools import lru_cache
from typing import List, Optional
from uuid import UUID

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from app.config import get_settings
from app.clients import place_client

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def _get_llm() -> ChatOllama:
    settings = get_settings()
    return ChatOllama(
        model=settings.ollama_model_assistant,
        base_url=settings.ollama_url,
        num_predict=60,
        temperature=0.3,
    )


_SYSTEM = """You are a helpful assistant on safeher, a women's safety platform.
Give users a short, warm, specific writing prompt (1 sentence, max 20 words) that
helps them write a more useful safety review.

Base the prompt on the score and tags they have selected.
Never be preachy. Focus on practical safety details.
Output the prompt sentence only. No preamble."""


async def get_writing_prompt(
    place_id: UUID,
    score: int,
    selected_tags: Optional[List[str]],
    partial_body: Optional[str],
) -> str:
    place = await place_client.get_place(place_id)

    user_message = (
        f"Place: {place.get('name')} ({place.get('category')})\n"
        f"Score selected: {score}/5\n"
        f"Tags selected: {', '.join(selected_tags) if selected_tags else 'none'}\n"
        f"Partial review text: {partial_body or '(none yet)'}"
    )

    chain = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM),
        ("human", "{user_message}"),
    ]) | _get_llm()

    try:
        result = await chain.ainvoke({"user_message": user_message})
        return result.content.strip()
    except Exception as e:
        logger.warning(f"SmartReviewAssistant failed for place [{place_id}]: {e}")
        return "What specific details would help someone feel confident about this place?"
