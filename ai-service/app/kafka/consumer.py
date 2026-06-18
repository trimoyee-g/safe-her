import asyncio
import json
import logging
from typing import Awaitable, Callable

from aiokafka import AIOKafkaConsumer

from app.config import get_settings

logger = logging.getLogger(__name__)


async def start(
    topics: list[str],
    handler: Callable[[str, dict], Awaitable[None]],
) -> asyncio.Task:
    settings = get_settings()
    consumer = AIOKafkaConsumer(
        *topics,
        bootstrap_servers=settings.kafka_bootstrap,
        group_id=settings.kafka_group_id,
        auto_offset_reset="earliest",
        enable_auto_commit=False,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
    )
    await consumer.start()
    logger.info(f"Kafka consumer started on topics: {topics}")

    async def _loop():
        try:
            async for msg in consumer:
                try:
                    await handler(msg.topic, msg.value)
                except Exception as e:
                    logger.error(f"Error handling message from {msg.topic}: {e}")
                finally:
                    # Always commit so a poison-pill message doesn't stall the consumer.
                    # A production system would route failures to a DLQ before committing.
                    await consumer.commit()
        finally:
            await consumer.stop()
            logger.info("Kafka consumer stopped")

    return asyncio.create_task(_loop())
