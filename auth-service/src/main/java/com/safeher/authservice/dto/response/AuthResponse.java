package com.safeher.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private UUID userId;
    private String username;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;   // seconds
    private long refreshTokenExpiresIn;  // seconds

    @Builder.Default
    private OffsetDateTime issuedAt = OffsetDateTime.now();
}
