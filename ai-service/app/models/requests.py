from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str
    history: Optional[List[ChatMessage]] = []
    placeIds: Optional[List[UUID]] = []


class ReviewAssistRequest(BaseModel):
    placeId: UUID
    score: int
    selectedTags: Optional[List[str]] = []
    partialBody: Optional[str] = None


class ModerationRequest(BaseModel):
    ratingId: str
    placeId: UUID
    userId: Optional[UUID] = None
    score: int
    title: Optional[str] = None
    body: Optional[str] = None
    tags: Optional[List[str]] = []
