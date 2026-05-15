package com.safeher.placeservice.controller;

import com.safeher.placeservice.dto.response.PlaceResponse;
import com.safeher.placeservice.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal endpoints consumed by other microservices via Feign.
 * Must NOT be exposed on the public API Gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/places")
@RequiredArgsConstructor
public class InternalPlaceController {

    private final PlaceService placeService;

    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable UUID id) {
        return placeService.getPlaceById(id);
    }

    /**
     * Called by Rating Service after recalculating the aggregated score.
     * Provides an alternative to the Kafka path for immediate consistency.
     */
    @PatchMapping("/{id}/score")
    public void updateScore(
            @PathVariable UUID id,
            @RequestParam BigDecimal score,
            @RequestParam int totalRatings) {
        placeService.updateSafetyScore(id, score, totalRatings);
    }
}
