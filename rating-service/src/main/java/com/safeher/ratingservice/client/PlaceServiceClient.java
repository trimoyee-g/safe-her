package com.safeher.ratingservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name     = "place-service",
    path     = "/api/v1/internal/places",
    fallback = PlaceServiceClient.Fallback.class
)
public interface PlaceServiceClient {

    @GetMapping("/{id}")
    PlaceInfo getPlace(@PathVariable UUID id);

    // ── Minimal DTO ───────────────────────────────────────────────────────────
    record PlaceInfo(UUID id, String name, boolean active) {}

    @Slf4j
    class Fallback implements PlaceServiceClient {
        @Override
        public PlaceInfo getPlace(UUID id) {
            log.warn("[Fallback] PlaceService unavailable for placeId={}", id);
            return new PlaceInfo(id, "Unknown Place", true); // assume active to not block writes
        }
    }
}
