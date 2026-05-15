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
public class UserUpdatedEvent {
    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;

    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
