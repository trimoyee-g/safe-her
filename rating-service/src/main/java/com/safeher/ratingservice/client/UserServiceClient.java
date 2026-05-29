package com.safeher.ratingservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(
    name     = "user-service",
    path     = "/api/v1/internal/users",
    fallback = UserServiceClient.Fallback.class
)
public interface UserServiceClient {

    @GetMapping("/{id}/public")
    UserInfo getPublicProfile(@PathVariable UUID id);

    @PostMapping("/{id}/increment-reviews")
    void incrementReviewCount(@PathVariable UUID id);

    record UserInfo(UUID id, String username, String displayName, String avatarUrl) {}

    @Component
    @Slf4j
    class Fallback implements UserServiceClient {
        @Override
        public UserInfo getPublicProfile(UUID id) {
            log.warn("[Fallback] UserService unavailable for userId={}", id);
            return new UserInfo(id, "user", "safeher User", null);
        }

        @Override
        public void incrementReviewCount(UUID id) {
            log.warn("[Fallback] Could not increment review count for userId={}", id);
        }
    }
}
