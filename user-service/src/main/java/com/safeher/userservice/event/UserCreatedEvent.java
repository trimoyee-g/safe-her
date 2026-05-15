package com.safeher.userservice.event;

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
public class UserCreatedEvent {
    private UUID userId;
    private UUID authUserId;
    private String username;
    private String email;
    private String role;

    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
