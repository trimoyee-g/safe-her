import logging
from uuid import UUID

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)


async def get_ratings_by_place(
    place_id: UUID,
    sort_by: str = "NEWEST",
    page: int = 0,
    size: int = 50,
) -> dict:
    settings = get_settings()
    url = f"{settings.rating_service_url}/api/v1/ratings/place/{place_id}"
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(url, params={"sortBy": sort_by, "page": page, "size": size})
            resp.raise_for_status()
            return resp.json()
    except Exception as e:
        logger.warning(f"RatingService unavailable for place={place_id}: {e}")
        return {"content": [], "totalPages": 0, "totalElements": 0}


async def suppress_rating(rating_id: str) -> None:
    settings = get_settings()
    url = f"{settings.rating_service_url}/api/v1/internal/ratings/{rating_id}/suppress"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            await client.patch(url)
    except Exception as e:
        logger.warning(f"Could not suppress rating {rating_id}: {e}")
