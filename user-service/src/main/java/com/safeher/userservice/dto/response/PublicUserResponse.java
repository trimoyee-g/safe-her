package com.safeher.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Stripped-down view exposed to unauthenticated callers and cross-service lookups.
 * Email, phone, location coordinates and auth IDs are intentionally omitted.
 */
@Data
@Builder
public class PublicUserResponse {

    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String city;
    private String country;
    private boolean verified;
    private int totalReviews;
    private int helpfulVotes;
}
