package com.safeher.authservice.event;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthPasswordChangedEvent {
    private UUID authUserId;
    @Builder.Default private OffsetDateTime occurredAt = OffsetDateTime.now();
}
