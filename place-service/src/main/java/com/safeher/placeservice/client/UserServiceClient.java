package com.safeher.placeservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(
    name      = "user-service",
    path      = "/api/v1/internal/users",
    fallback  = UserServiceClient.UserServiceFallback.class
)
public interface UserServiceClient {

    @GetMapping("/by-auth/{authUserId}")
    Boolean existsByAuthUserId(@PathVariable UUID authUserId);

    @PostMapping("/{userId}/increment-reviews")
    void incrementReviewCount(@PathVariable UUID userId);

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Slf4j
    class UserServiceFallback implements UserServiceClient {

        @Override
        public Boolean existsByAuthUserId(UUID authUserId) {
            log.warn("[Fallback] existsByAuthUserId for {} – returning true to avoid rejecting requests", authUserId);
            return true;
        }

        @Override
        public void incrementReviewCount(UUID userId) {
            log.warn("[Fallback] incrementReviewCount for {} – skipped", userId);
        }
    }
}
