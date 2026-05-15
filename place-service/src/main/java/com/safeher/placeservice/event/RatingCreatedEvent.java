package com.safeher.placeservice.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Event published by Rating Service when a new rating is posted. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingCreatedEvent {
    private UUID ratingId;
    private UUID placeId;
    private UUID userId;
    private int score;               // 1-5
    private double newAverageScore;
    private int totalRatings;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
