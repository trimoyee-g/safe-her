import asyncio
import logging
from typing import Optional
from uuid import UUID

from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings
from langchain_postgres.vectorstores import PGVector

from app.config import get_settings

logger = logging.getLogger(__name__)

COLLECTION_NAME = "safeher_reviews"
BATCH_SIZE = 50

_store: Optional[PGVector] = None
_batch: list = []
_batch_lock: Optional[asyncio.Lock] = None


async def setup() -> None:
    """Create pgvector tables and warm the module-level store. Call once at startup."""
    global _store, _batch_lock
    settings = get_settings()
    embeddings = OllamaEmbeddings(
        model=settings.ollama_model_embed,
        base_url=settings.ollama_url,
    )
    store = PGVector(
        connection=settings.vector_db_url,
        embeddings=embeddings,
        collection_name=COLLECTION_NAME,
        async_mode=True,
    )
    await store.acreate_tables_if_not_exists()
    await store.acreate_collection()
    _store = store
    _batch_lock = asyncio.Lock()
    logger.info("PGVector store initialised (collection=%s)", COLLECTION_NAME)


async def enqueue(event) -> None:
    """Add a RatingCreatedEvent to the pending batch; flush immediately when batch hits BATCH_SIZE."""
    if _store is None or _batch_lock is None:
        return
    to_flush = None
    async with _batch_lock:
        _batch.append(event)
        if len(_batch) >= BATCH_SIZE:
            to_flush = _batch.copy()
            _batch.clear()
    if to_flush:
        asyncio.create_task(_flush(to_flush))


async def periodic_flush_loop(interval_seconds: int = 1200) -> None:
    """Flush any partial batch on a timer so events never stall indefinitely."""
    while True:
        await asyncio.sleep(interval_seconds)
        if _batch_lock is None or not _batch:
            continue
        async with _batch_lock:
            if not _batch:
                continue
            to_flush = _batch.copy()
            _batch.clear()
        asyncio.create_task(_flush(to_flush))


async def _flush(events: list) -> None:
    from app.clients import place_client, rating_client

    if _store is None or not events:
        return

    # Fetch every review + place concurrently across the whole batch in one gather
    pairs = await asyncio.gather(
        *[
            asyncio.gather(
                rating_client.get_rating(e.ratingId),
                place_client.get_place(e.placeId),
                return_exceptions=True,
            )
            for e in events
        ]
    )

    docs: list[Document] = []
    ids: list[str] = []
    for event, (rating, place) in zip(events, pairs):
        if isinstance(rating, Exception) or isinstance(place, Exception):
            logger.warning("Skipping review %s in batch flush: fetch failed", event.ratingId)
            continue
        body = (rating.get("body") or "").strip()
        if not body:
            continue
        tags = rating.get("tags") or []
        tag_str = ", ".join(tags) if tags else "none"
        place_name = place.get("name", "Unknown")
        category = place.get("category", "OTHER")
        city = place.get("city") or place.get("country") or "Unknown"
        docs.append(Document(
            page_content=(
                f"{place_name} ({category}) in {city}: "
                f"safety score {event.score}/5, tags [{tag_str}]. {body}"
            ),
            metadata={
                "rating_id": event.ratingId,
                "place_id": str(event.placeId),
                "place_name": place_name,
                "category": category,
                "city": city,
                "score": event.score,
            },
        ))
        ids.append(event.ratingId)

    if not docs:
        return
    try:
        await _store.aadd_documents(docs, ids=ids)
        logger.info("Vector batch flushed: %d/%d docs embedded", len(docs), len(events))
    except Exception as e:
        logger.warning("Vector batch flush failed: %s", e)


async def search_places(query: str, top_k: int = 5) -> list[UUID]:
    """Return up to top_k unique place_ids whose reviews best match query."""
    if _store is None:
        return []
    try:
        docs = await _store.asimilarity_search(query, k=top_k * 4)
        seen: set[str] = set()
        place_ids: list[UUID] = []
        for doc in docs:
            pid = doc.metadata.get("place_id")
            if pid and pid not in seen:
                seen.add(pid)
                place_ids.append(UUID(pid))
                if len(place_ids) >= top_k:
                    break
        return place_ids
    except Exception as e:
        logger.warning("Vector search failed: %s", e)
        return []
