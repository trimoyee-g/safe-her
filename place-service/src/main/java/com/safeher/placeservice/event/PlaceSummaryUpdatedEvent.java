package com.safeher.placeservice.event;

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
public class PlaceSummaryUpdatedEvent {
    private UUID placeId;
    private String summary;
    private int reviewsAnalysed;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
