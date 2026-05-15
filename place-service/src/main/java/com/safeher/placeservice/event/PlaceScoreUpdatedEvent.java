package com.safeher.placeservice.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlaceScoreUpdatedEvent {
    private UUID placeId;
    private BigDecimal newSafetyScore;
    private int totalRatings;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
