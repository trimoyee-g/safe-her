import logging
from uuid import UUID

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)

_FALLBACK_PLACE = {
    "name": "Unknown",
    "category": "OTHER",
    "address": None,
    "city": None,
    "country": None,
    "safetyScore": 0.0,
    "totalRatings": 0,
    "verified": False,
    "description": None,
}


async def get_place(place_id: UUID) -> dict:
    settings = get_settings()
    url = f"{settings.place_service_url}/api/v1/places/{place_id}"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(url)
            resp.raise_for_status()
            body = resp.json()
            # Unwrap ApiResponse<PlaceDto> wrapper if present
            return body.get("data", body)
    except Exception as e:
        logger.warning(f"PlaceService unavailable for place={place_id}: {e}")
        return {**_FALLBACK_PLACE, "id": str(place_id)}


async def update_description(place_id: UUID, description: str) -> None:
    settings = get_settings()
    url = f"{settings.place_service_url}/api/v1/internal/places/{place_id}/description"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            await client.patch(url, json={"description": description})
    except Exception as e:
        logger.warning(f"Could not save AI description for place={place_id}: {e}")


async def update_ai_summary(place_id: UUID, summary: str, generated_at: str) -> None:
    settings = get_settings()
    url = f"{settings.place_service_url}/api/v1/internal/places/{place_id}/ai-summary"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            await client.patch(url, json={"summary": summary, "generatedAt": generated_at})
    except Exception as e:
        logger.warning(f"Could not save AI summary for place={place_id}: {e}")
