package com.safeher.ratingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFlaggedEvent {
    private String          ratingId;
    private UUID            placeId;
    private UUID            userId;
    private String          reason;         // SPAM | ABUSE | FAKE | COORDINATED
    private double          confidence;     // 0.0 – 1.0
    private boolean         autoSuppressed;
    @Builder.Default
    private OffsetDateTime  occurredAt = OffsetDateTime.now();
}
