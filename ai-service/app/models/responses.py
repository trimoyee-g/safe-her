from datetime import datetime
from typing import Any, Generic, List, Optional, TypeVar
from uuid import UUID

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    success: bool
    message: str
    data: Optional[T] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)

    @classmethod
    def ok(cls, data: Any = None, message: str = "OK") -> "ApiResponse":
        return cls(success=True, message=message, data=data)

    @classmethod
    def error(cls, message: str) -> "ApiResponse":
        return cls(success=False, message=message, data=None)


class Source(BaseModel):
    """One citation backing a chatbot answer — either a first-party review
    aggregate or a web result. `kind` lets the frontend render them differently
    (e.g. a verified badge for "review", an external-link icon for "web")."""
    kind: str  # "review" | "web"
    label: str  # e.g. "3 reviews of Blue Tokai Cafe" or the web page title
    url: Optional[str] = None  # present for kind="web"
    placeId: Optional[UUID] = None  # present for kind="review"


class ChatResponse(BaseModel):
    message: str
    suggestedPlaceIds: Optional[List[UUID]] = []
    sources: List[Source] = []
    # "high"       — grounded in our own review data
    # "medium"     — some first-party data, supplemented by web results
    # "low"        — web-only, little or no first-party data
    # "no_data"    — nothing specific found; answer is general safety advice
    confidence: str = "no_data"
