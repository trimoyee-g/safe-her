package com.safeher.placeservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Thin wrapper around the Google Places Nearby Search API.
 * Used only during seeding – not in the hot read path.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesClient {

    private final RestTemplate restTemplate;

    @Value("${app.external.google-places.api-key:}") private String apiKey;
    @Value("${app.external.google-places.base-url}")  private String baseUrl;

    @CircuitBreaker(name = "google-places", fallbackMethod = "nearbySearchFallback")
    @Retry(name = "google-places")
    public List<GooglePlaceResult> nearbySearch(double lat, double lng,
                                                 int radiusMeters, String type) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Places API key not configured – skipping seed");
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/nearbysearch/json")
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radiusMeters)
                .queryParam("type", type)
                .queryParam("key", apiKey)
                .toUriString();

        GooglePlacesResponse response = restTemplate.getForObject(url, GooglePlacesResponse.class);
        if (response == null || response.getResults() == null) return Collections.emptyList();
        return response.getResults();
    }

    public List<GooglePlaceResult> nearbySearchFallback(double lat, double lng,
                                                         int radiusMeters, String type,
                                                         Exception ex) {
        log.error("Google Places circuit open or error: {}", ex.getMessage());
        return Collections.emptyList();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePlacesResponse {
        private List<GooglePlaceResult> results;
        private String status;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePlaceResult {
        @JsonProperty("place_id") private String placeId;
        private String name;
        private String vicinity;
        private Geometry geometry;
        private List<String> types;
        @JsonProperty("business_status") private String businessStatus;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Geometry {
            private Location location;
            @Data @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Location {
                private double lat;
                private double lng;
            }
        }
    }
}
