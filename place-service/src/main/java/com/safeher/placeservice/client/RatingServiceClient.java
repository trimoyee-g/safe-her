package com.safeher.placeservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(
    name     = "rating-service",
    path     = "/api/v1/internal/ratings",
    fallback = RatingServiceClient.RatingServiceFallback.class
)
public interface RatingServiceClient {

    /** Fetch current aggregate score for a place (used on first place fetch if score is 0). */
    @GetMapping("/places/{placeId}/score")
    BigDecimal getAggregatedScore(@PathVariable UUID placeId);

    @Slf4j
    class RatingServiceFallback implements RatingServiceClient {
        @Override
        public BigDecimal getAggregatedScore(UUID placeId) {
            log.warn("[Fallback] getAggregatedScore for place {} – returning 0", placeId);
            return BigDecimal.ZERO;
        }
    }
}
