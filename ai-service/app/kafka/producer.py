import json
import logging
from typing import Any

from aiokafka import AIOKafkaProducer

from app.config import get_settings

logger = logging.getLogger(__name__)

_producer: AIOKafkaProducer | None = None


async def start() -> None:
    global _producer
    settings = get_settings()
    _producer = AIOKafkaProducer(
        bootstrap_servers=settings.kafka_bootstrap,
        value_serializer=lambda v: json.dumps(v, default=str).encode(),
        key_serializer=lambda k: k.encode() if k else None,
        acks="all",
    )
    await _producer.start()
    logger.info("Kafka producer started")


async def stop() -> None:
    if _producer:
        await _producer.stop()
        logger.info("Kafka producer stopped")


async def send(topic: str, key: str, value: Any) -> None:
    if not _producer:
        logger.error("Kafka producer not initialized")
        return
    try:
        if hasattr(value, "model_dump"):
            value = value.model_dump(mode="json")
        await _producer.send_and_wait(topic, key=key, value=value)
    except Exception as e:
        logger.error(f"Failed to send Kafka message to {topic}: {e}")
