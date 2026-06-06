from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class RatingCreatedEvent(BaseModel):
    ratingId: str
    placeId: UUID
    userId: Optional[UUID] = None
    score: int
    newAverageScore: float = 0.0
    totalRatings: int = 0
    occurredAt: Optional[datetime] = None


class PlaceCreatedEvent(BaseModel):
    placeId: UUID
    name: str
    category: Optional[str] = None
    latitude: float = 0.0
    longitude: float = 0.0
    city: Optional[str] = None
    country: Optional[str] = None
    createdBy: Optional[UUID] = None
    occurredAt: Optional[datetime] = None


class ReviewFlaggedEvent(BaseModel):
    ratingId: str
    placeId: Optional[UUID] = None
    userId: Optional[UUID] = None
    reason: str
    confidence: float
    autoSuppressed: bool = False
    occurredAt: datetime = Field(default_factory=datetime.utcnow)


class PlaceSummaryUpdatedEvent(BaseModel):
    placeId: UUID
    summary: str
    reviewsAnalysed: int
    occurredAt: datetime = Field(default_factory=datetime.utcnow)


class PlaceDescriptionUpdatedEvent(BaseModel):
    placeId: UUID
    description: str
    occurredAt: datetime = Field(default_factory=datetime.utcnow)
