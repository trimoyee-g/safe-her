package com.safeher.placeservice.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlaceUpdatedEvent {
    private UUID placeId;
    private String name;
    private String category;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
