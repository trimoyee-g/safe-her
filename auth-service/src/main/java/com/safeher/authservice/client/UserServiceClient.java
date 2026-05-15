package com.safeher.authservice.client;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
    name     = "user-service",
    path     = "/api/v1/users",
    fallback = UserServiceClient.UserServiceFallback.class
)
public interface UserServiceClient {

    @PostMapping
    void createUserProfile(@RequestBody CreateProfileRequest request);

    // ── Request payload ───────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    class CreateProfileRequest {
        private UUID   authUserId;
        private String username;
        private String email;
        private String displayName;
        private String city;
        private String country;
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Slf4j
    class UserServiceFallback implements UserServiceClient {
        @Override
        public void createUserProfile(CreateProfileRequest request) {
            log.error("[Fallback] User Service unavailable – profile creation deferred for authUserId={}. " +
                      "The auth.user.registered Kafka event will allow User Service to catch up.", request.getAuthUserId());
        }
    }
}
