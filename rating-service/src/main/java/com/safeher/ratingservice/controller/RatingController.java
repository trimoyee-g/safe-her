package com.safeher.ratingservice.controller;

import com.safeher.ratingservice.dto.request.CreateRatingRequest;
import com.safeher.ratingservice.dto.request.KeywordSearchRequest;
import com.safeher.ratingservice.dto.request.UpdateRatingRequest;
import com.safeher.ratingservice.dto.response.*;
import com.safeher.ratingservice.enums.RatingSortBy;
import com.safeher.ratingservice.service.RatingService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // ── Public read ───────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @RateLimiter(name = "rating-api")
    public ApiResponse<RatingResponse> getById(@PathVariable String id,
                                                Authentication auth) {
        UUID callerId = extractUserId(auth);
        boolean isAdmin = isAdmin(auth);
        return ApiResponse.ok(ratingService.getRatingById(id, callerId, isAdmin));
    }

    @GetMapping("/place/{placeId}")
    @RateLimiter(name = "rating-api")
    public ApiResponse<PagedResponse<RatingResponse>> getByPlace(
            @PathVariable UUID placeId,
            @RequestParam(defaultValue = "NEWEST") RatingSortBy sortBy,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID callerId = extractUserId(auth);
        return ApiResponse.ok(ratingService.getRatingsByPlace(placeId, sortBy, page, size, callerId, isAdmin(auth)));
    }

    @GetMapping("/place/{placeId}/summary")
    @RateLimiter(name = "rating-api")
    public ApiResponse<PlaceRatingSummary> getSummary(@PathVariable UUID placeId) {
        return ApiResponse.ok(ratingService.getSummaryForPlace(placeId));
    }

    @GetMapping("/user/{userId}")
    @RateLimiter(name = "rating-api")
    public ApiResponse<PagedResponse<RatingResponse>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID callerId = extractUserId(auth);
        return ApiResponse.ok(ratingService.getRatingsByUser(userId, page, size, callerId, isAdmin(auth)));
    }

    @GetMapping("/search")
    @RateLimiter(name = "rating-api")
    public ApiResponse<PagedResponse<RatingResponse>> search(
            @Valid KeywordSearchRequest request, Authentication auth) {
        UUID callerId = extractUserId(auth);
        return ApiResponse.ok(ratingService.keywordSearch(request, callerId, isAdmin(auth)));
    }

    // ── Authenticated writes ──────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RatingResponse> create(@Valid @RequestBody CreateRatingRequest request,
                                               Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        return ApiResponse.ok("Review posted", ratingService.createRating(request, callerId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RatingResponse> update(@PathVariable String id,
                                               @Valid @RequestBody UpdateRatingRequest request,
                                               Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        return ApiResponse.ok("Review updated", ratingService.updateRating(id, request, callerId, isAdmin(auth)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable String id, Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        ratingService.deleteRating(id, callerId, isAdmin(auth));
        return ApiResponse.ok("Review deleted", null);
    }

    // ── Engagement ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/helpful")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markHelpful(@PathVariable String id, Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        ratingService.markHelpful(id, callerId);
        return ApiResponse.ok("Vote recorded", null);
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> report(@PathVariable String id, Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        ratingService.reportRating(id, callerId);
        return ApiResponse.ok("Review reported", null);
    }

    // ── My reviews ────────────────────────────────────────────────────────────

    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PagedResponse<RatingResponse>> myReviews(
            Authentication auth,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID callerId = (UUID) auth.getCredentials();
        return ApiResponse.ok(ratingService.getRatingsByUser(callerId, page, size, callerId, false));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID extractUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object cred = auth.getCredentials();
        return (cred instanceof UUID uuid) ? uuid : null;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_MODERATOR"));
    }
}
