package com.safeher.placeservice.controller;

import com.safeher.placeservice.dto.request.CreatePlaceRequest;
import com.safeher.placeservice.dto.request.GeoSearchRequest;
import com.safeher.placeservice.dto.request.KeywordSearchRequest;
import com.safeher.placeservice.dto.request.UpdatePlaceRequest;
import com.safeher.placeservice.dto.response.ApiResponse;
import com.safeher.placeservice.dto.response.PagedResponse;
import com.safeher.placeservice.dto.response.PlaceResponse;
import com.safeher.placeservice.dto.response.PlaceSummaryResponse;
import com.safeher.placeservice.enums.PlaceCategory;
import com.safeher.placeservice.service.PlaceService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // ── Public read – no auth required ────────────────────────────────────────

    @GetMapping("/{id}")
    @RateLimiter(name = "place-api")
    public ApiResponse<PlaceResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(placeService.getPlaceById(id));
    }

    @GetMapping
    @RateLimiter(name = "place-api")
    public ApiResponse<PagedResponse<PlaceSummaryResponse>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(placeService.listPlaces(page, size));
    }

    @GetMapping("/category/{category}")
    @RateLimiter(name = "place-api")
    public ApiResponse<PagedResponse<PlaceSummaryResponse>> listByCategory(
            @PathVariable PlaceCategory category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(placeService.listByCategory(category, page, size));
    }

    @GetMapping("/top-rated")
    public ApiResponse<List<PlaceSummaryResponse>> topRated() {
        return ApiResponse.ok(placeService.getTopRatedPlaces());
    }

    // ── Geo-search ────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/places/geo-search?latitude=12.9716&longitude=77.5946&radiusMeters=3000&category=RESTAURANT&page=0&size=20
     */
    @GetMapping("/geo-search")
    @RateLimiter(name = "geo-search")
    public ApiResponse<PagedResponse<PlaceSummaryResponse>> geoSearch(
            @Valid GeoSearchRequest request) {
        return ApiResponse.ok(placeService.geoSearch(request));
    }

    // ── Keyword search (Elasticsearch) ────────────────────────────────────────

    /**
     * GET /api/v1/places/search?query=coffee&city=Kolkata&category=CAFE&page=0&size=20
     */
    @GetMapping("/search")
    @RateLimiter(name = "place-api")
    public ApiResponse<PagedResponse<PlaceSummaryResponse>> keywordSearch(
            @Valid KeywordSearchRequest request) {
        return ApiResponse.ok(placeService.keywordSearch(request));
    }

    // ── Authenticated – create / update / delete ──────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PlaceResponse> createPlace(
            @Valid @RequestBody CreatePlaceRequest request,
            Authentication auth) {
        UUID callerUserId = (UUID) auth.getCredentials();
        return ApiResponse.ok("Place created", placeService.createPlace(request, callerUserId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PlaceResponse> updatePlace(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlaceRequest request,
            Authentication auth) {
        UUID callerUserId = (UUID) auth.getCredentials();
        boolean isAdmin   = isAdmin(auth);
        return ApiResponse.ok("Place updated", placeService.updatePlace(id, request, callerUserId, isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deletePlace(
            @PathVariable UUID id,
            Authentication auth) {
        UUID callerUserId = (UUID) auth.getCredentials();
        boolean isAdmin   = isAdmin(auth);
        placeService.deletePlace(id, callerUserId, isAdmin);
        return ApiResponse.ok("Place deleted", null);
    }

    // ── My places ─────────────────────────────────────────────────────────────

    @GetMapping("/my-places")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PagedResponse<PlaceSummaryResponse>> myPlaces(
            Authentication auth,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID callerUserId = (UUID) auth.getCredentials();
        return ApiResponse.ok(placeService.listByUser(callerUserId, page, size));
    }

    // ── Report ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> report(@PathVariable UUID id, Authentication auth) {
        UUID callerUserId = (UUID) auth.getCredentials();
        placeService.reportPlace(id, callerUserId);
        return ApiResponse.ok("Place reported", null);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlaceResponse> verify(@PathVariable UUID id) {
        return ApiResponse.ok("Place verified", placeService.verifyPlace(id));
    }

    @DeleteMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminDelete(@PathVariable UUID id) {
        placeService.adminDeletePlace(id);
        return ApiResponse.ok("Place deleted by admin", null);
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    @PostMapping("/seed/google")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> seedFromGoogle(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") int radiusMeters,
            @RequestParam(defaultValue = "establishment") String type,
            Authentication auth) {
        UUID callerUserId = (UUID) auth.getCredentials();
        int count = placeService.seedFromGooglePlaces(lat, lng, radiusMeters, type, callerUserId);
        return ApiResponse.ok("Seeded " + count + " places", count);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_MODERATOR"));
    }
}
