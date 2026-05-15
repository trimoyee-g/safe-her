package com.safeher.placeservice.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlaceCreatedEvent {
    private UUID placeId;
    private String name;
    private String category;
    private double latitude;
    private double longitude;
    private String city;
    private String country;
    private UUID createdBy;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
