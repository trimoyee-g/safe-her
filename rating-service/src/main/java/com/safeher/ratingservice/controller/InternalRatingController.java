package com.safeher.ratingservice.controller;

import com.safeher.ratingservice.dto.response.PagedResponse;
import com.safeher.ratingservice.dto.response.PlaceRatingSummary;
import com.safeher.ratingservice.dto.response.RatingResponse;
import com.safeher.ratingservice.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal endpoints consumed by Place Service via Feign.
 * Must NOT be reachable from the public internet (blocked at API Gateway).
 */
@RestController
@RequestMapping("/api/v1/internal/ratings")
@RequiredArgsConstructor
public class InternalRatingController {

    private final RatingService ratingService;

    @GetMapping("/places/{placeId}/score")
    public BigDecimal getAggregatedScore(@PathVariable UUID placeId) {
        return ratingService.getAggregatedScore(placeId);
    }

    @GetMapping("/places/{placeId}/summary")
    public PlaceRatingSummary getSummary(@PathVariable UUID placeId) {
        return ratingService.getSummaryForPlace(placeId);
    }

    @PatchMapping("/{id}/suppress")
    public void suppress(@PathVariable String id) {
        ratingService.suppress(id);
    }

    @PatchMapping("/{id}/flag")
    public void flag(@PathVariable String id,
                     @RequestParam String reason,
                     @RequestParam double confidence) {
        ratingService.flag(id, reason, confidence);
    }

    @GetMapping("/flagged")
    public PagedResponse<RatingResponse> getFlagged(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ratingService.getFlaggedReviews(page, size);
    }
}
