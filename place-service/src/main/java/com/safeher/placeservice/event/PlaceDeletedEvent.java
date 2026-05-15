package com.safeher.placeservice.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlaceDeletedEvent {
    private UUID placeId;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
