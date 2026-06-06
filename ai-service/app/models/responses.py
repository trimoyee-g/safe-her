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


class ChatResponse(BaseModel):
    message: str
    suggestedPlaceIds: Optional[List[UUID]] = []
