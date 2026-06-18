import asyncio
import logging
from contextlib import asynccontextmanager

import redis.asyncio as aioredis
from fastapi import FastAPI

from app.config import get_settings
from app.kafka import consumer as kafka_consumer
from app.kafka import producer as kafka_producer
from app.routers import ai as ai_router
from app.agents import anomaly_detector, chatbot, summarizer
from app.models.events import PlaceCreatedEvent, RatingCreatedEvent
from app import vector_store

logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s %(levelname)s %(name)s – %(message)s",
)
logger = logging.getLogger(__name__)


async def _on_kafka_event(topic: str, data: dict) -> None:
    """Route Kafka messages to the appropriate agents (fire-and-forget)."""
    from app.agents import description, moderation

    settings = get_settings()
    if topic == settings.kafka_topic_rating_created:
        try:
            event = RatingCreatedEvent(**data)
        except Exception as e:
            logger.error(f"Failed to parse RatingCreatedEvent: {e}")
            return
        asyncio.create_task(moderation.moderate(
            rating_id=event.ratingId,
            place_id=event.placeId,
            user_id=event.userId,
            score=event.score,
            title=None,
            body=None,
            tags=None,
        ))
        asyncio.create_task(summarizer.maybe_regenerate(event.placeId, event.totalRatings))
        asyncio.create_task(anomaly_detector.check_velocity(event.ratingId, event.placeId))
        asyncio.create_task(vector_store.enqueue(event))

    elif topic == settings.kafka_topic_place_created:
        try:
            event = PlaceCreatedEvent(**data)
        except Exception as e:
            logger.error(f"Failed to parse PlaceCreatedEvent: {e}")
            return
        asyncio.create_task(description.generate_for_new_place(event.placeId))


_consumer_task: asyncio.Task | None = None
_flush_task: asyncio.Task | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _consumer_task, _flush_task
    settings = get_settings()

    # Vector store
    try:
        await vector_store.setup()
        _flush_task = asyncio.create_task(vector_store.periodic_flush_loop())
    except Exception as e:
        logger.warning("Vector store setup failed (discovery queries will be unavailable): %s", e)

    # Redis
    redis_client = aioredis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        password=settings.redis_password or None,
        decode_responses=True,
    )
    summarizer.set_redis(redis_client)
    anomaly_detector.set_redis(redis_client)
    chatbot.set_redis(redis_client)
    app.state.redis = redis_client
    logger.info("Redis client configured")

    # Kafka producer
    await kafka_producer.start()

    # Kafka consumer
    _consumer_task = await kafka_consumer.start(
        topics=[settings.kafka_topic_rating_created, settings.kafka_topic_place_created],
        handler=_on_kafka_event,
    )

    # Eureka — register so the Spring Cloud Gateway can discover this service
    try:
        import py_eureka_client.eureka_client as eureka_client
        await eureka_client.init_async(
            eureka_server=settings.eureka_uri,
            app_name="AI-SERVICE",
            instance_port=settings.port,
            renewal_interval_in_secs=30,
            duration_in_secs=90,
        )
        logger.info("Registered with Eureka")
    except Exception as e:
        logger.warning(f"Eureka registration skipped: {e}")

    logger.info("AI Service started on port %d", settings.port)
    yield

    # Graceful shutdown
    for task in (_consumer_task, _flush_task):
        if task:
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass

    await kafka_producer.stop()
    await redis_client.aclose()
    logger.info("AI Service shutdown complete")


app = FastAPI(
    title="SafeHer AI Service",
    version="2.0.0",
    lifespan=lifespan,
)
app.include_router(ai_router.router)
