package com.safeher.ratingservice.service;

import com.safeher.ratingservice.dto.request.CreateRatingRequest;
import com.safeher.ratingservice.dto.request.KeywordSearchRequest;
import com.safeher.ratingservice.dto.request.UpdateRatingRequest;
import com.safeher.ratingservice.dto.response.PagedResponse;
import com.safeher.ratingservice.dto.response.PlaceRatingSummary;
import com.safeher.ratingservice.dto.response.RatingResponse;
import com.safeher.ratingservice.enums.RatingSortBy;

import java.math.BigDecimal;
import java.util.UUID;

public interface RatingService {

    // ── CRUD ──────────────────────────────────────────────────────────────────
    RatingResponse createRating(CreateRatingRequest request, UUID callerUserId);

    RatingResponse getRatingById(String id, UUID callerUserId, boolean isAdmin);

    RatingResponse updateRating(String id, UpdateRatingRequest request, UUID callerUserId, boolean isAdmin);

    void deleteRating(String id, UUID callerUserId, boolean isAdmin);

    // ── Listing ───────────────────────────────────────────────────────────────
    PagedResponse<RatingResponse> getRatingsByPlace(UUID placeId, RatingSortBy sortBy,
                                                     int page, int size,
                                                     UUID callerUserId, boolean isAdmin);

    PagedResponse<RatingResponse> getRatingsByUser(UUID userId, int page, int size,
                                                    UUID callerUserId, boolean isAdmin);

    // ── Search ────────────────────────────────────────────────────────────────
    PagedResponse<RatingResponse> keywordSearch(KeywordSearchRequest request,
                                                 UUID callerUserId, boolean isAdmin);

    // ── Aggregation ───────────────────────────────────────────────────────────
    PlaceRatingSummary getSummaryForPlace(UUID placeId);

    BigDecimal getAggregatedScore(UUID placeId);   // internal – called by Place Service Feign

    // ── Engagement ────────────────────────────────────────────────────────────
    void markHelpful(String ratingId, UUID callerUserId);

    void reportRating(String ratingId, UUID callerUserId);
}
