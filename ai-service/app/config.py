from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    port: int = 8085

    ollama_url: str = "http://localhost:11434"
    ollama_model_moderation: str = "phi3:mini"
    ollama_model_summarization: str = "llama3.1:8b"
    ollama_model_anomaly: str = "phi3:mini"
    ollama_model_assistant: str = "phi3:mini"
    ollama_model_chatbot: str = "mistral:7b"
    ollama_model_description: str = "phi3:mini"
    ollama_timeout: int = 60

    kafka_bootstrap: str = "localhost:9092"
    kafka_group_id: str = "ai-service-group"
    kafka_topic_rating_created: str = "rating.created"
    kafka_topic_place_created: str = "place.created"
    kafka_topic_review_flagged: str = "ai.review.flagged"
    kafka_topic_place_summary_updated: str = "ai.place.summary.updated"
    kafka_topic_place_description_updated: str = "ai.place.description.updated"

    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""

    jwt_secret: str | None = None

    place_service_url: str = "http://place-service:8083"
    rating_service_url: str = "http://rating-service:8084"

    eureka_uri: str = "http://admin:admin@localhost:8761/eureka"

    ollama_model_embed: str = "nomic-embed-text"
    vector_db_url: str = "postgresql+psycopg://safeher:safeher@localhost:5432/safeher_vectors"

    moderation_auto_suppress_threshold: float = 0.90
    moderation_flag_threshold: float = 0.65
    summarizer_min_reviews: int = 5
    summarizer_regenerate_every: int = 10
    anomaly_velocity_window_minutes: int = 60
    anomaly_velocity_threshold: int = 15

    # Web Research Agent — cold-start bridge (see agents/web_research.py)
    web_search_enabled: bool = True
    web_search_max_results: int = 5
    web_search_timeout_s: float = 8.0
    # Below this many first-party reviews across the referenced places,
    # the synthesis agent leans on web research and lowers its confidence label.
    chatbot_first_party_confidence_floor: int = 3


@lru_cache
def get_settings() -> Settings:
    return Settings()
