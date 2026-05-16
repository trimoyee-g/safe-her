package com.safeher.aiservice.event;

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
public class PlaceDescriptionUpdatedEvent {
    private UUID placeId;
    private String description;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
