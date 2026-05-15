package com.safeher.ratingservice.event;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingDeletedEvent {
    private String ratingId;
    private UUID placeId;
    private double newAverageScore;
    private int totalRatings;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
