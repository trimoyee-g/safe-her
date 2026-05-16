package com.safeher.aiservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "place-service", path = "/api/v1",
             fallback = PlaceServiceClient.Fallback.class)
public interface PlaceServiceClient {

    @GetMapping("/places/{id}")
    PlaceDto getPlace(@PathVariable UUID id);

    @PatchMapping("/internal/places/{id}/ai-summary")
    void updateAiSummary(@PathVariable UUID id, @RequestBody AiSummaryUpdate update);

    record PlaceDto(UUID id, String name, String category,
                    String address, String city, String country,
                    double safetyScore, int totalRatings, boolean verified) {}

    record AiSummaryUpdate(String summary, String generatedAt) {}

    @Slf4j
    class Fallback implements PlaceServiceClient {
        @Override public PlaceDto getPlace(UUID id) {
            log.warn("[Fallback] PlaceService unavailable for place={}", id);
            return new PlaceDto(id, "Unknown", "OTHER", null, null, null, 0, 0, false);
        }
        @Override public void updateAiSummary(UUID id, AiSummaryUpdate u) {
            log.warn("[Fallback] Could not update AI summary for place={}", id);
        }
    }
}
