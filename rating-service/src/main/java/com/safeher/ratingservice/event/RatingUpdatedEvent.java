package com.safeher.ratingservice.event;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingUpdatedEvent {
    private String ratingId;
    private UUID placeId;
    private int newScore;
    private double newAverageScore;
    private int totalRatings;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
