package com.safeher.aiservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// ── Rating Service client ─────────────────────────────────────────────────────

@FeignClient(name = "rating-service", path = "/api/v1",
             fallback = RatingServiceClient.Fallback.class)
public interface RatingServiceClient {

    @GetMapping("/ratings/place/{placeId}")
    PagedRatings getRatingsByPlace(@PathVariable UUID placeId,
                                   @RequestParam(defaultValue = "NEWEST") String sortBy,
                                   @RequestParam(defaultValue = "0")  int page,
                                   @RequestParam(defaultValue = "50") int size);

    @GetMapping("/internal/ratings/places/{placeId}/summary")
    PlaceSummary getPlaceSummary(@PathVariable UUID placeId);

    @PatchMapping("/ratings/{id}/suppress")
    void suppressRating(@PathVariable String id);

    record PagedRatings(List<RatingDto> content, int totalPages, long totalElements) {}

    record RatingDto(String id, UUID placeId, UUID userId, int score,
                     String title, String body, List<String> tags,
                     boolean anonymous, int helpfulCount, int reportedCount,
                     String createdAt) {}

    record PlaceSummary(UUID placeId, BigDecimal averageScore, int totalRatings) {}

    @Slf4j
    class Fallback implements RatingServiceClient {
        @Override public PagedRatings getRatingsByPlace(UUID p, String s, int pg, int sz) {
            log.warn("[Fallback] RatingService unavailable for place={}", p);
            return new PagedRatings(List.of(), 0, 0);
        }
        @Override public PlaceSummary getPlaceSummary(UUID p) {
            return new PlaceSummary(p, BigDecimal.ZERO, 0);
        }
        @Override public void suppressRating(String id) {
            log.warn("[Fallback] Could not suppress rating {}", id);
        }
    }
}
