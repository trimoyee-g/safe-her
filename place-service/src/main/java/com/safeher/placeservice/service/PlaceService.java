package com.safeher.placeservice.service;

import com.safeher.placeservice.dto.request.CreatePlaceRequest;
import com.safeher.placeservice.dto.request.GeoSearchRequest;
import com.safeher.placeservice.dto.request.KeywordSearchRequest;
import com.safeher.placeservice.dto.request.UpdatePlaceRequest;
import com.safeher.placeservice.dto.response.PagedResponse;
import com.safeher.placeservice.dto.response.PlaceResponse;
import com.safeher.placeservice.dto.response.PlaceSummaryResponse;
import com.safeher.placeservice.enums.PlaceCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PlaceService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    PlaceResponse createPlace(CreatePlaceRequest request, UUID callerUserId);

    PlaceResponse getPlaceById(UUID id);

    PlaceResponse updatePlace(UUID id, UpdatePlaceRequest request, UUID callerUserId, boolean isAdmin);

    void deletePlace(UUID id, UUID callerUserId, boolean isAdmin);

    // ── Geo-search ────────────────────────────────────────────────────────────

    PagedResponse<PlaceSummaryResponse> geoSearch(GeoSearchRequest request);

    // ── Keyword search (Elasticsearch) ────────────────────────────────────────

    PagedResponse<PlaceSummaryResponse> keywordSearch(KeywordSearchRequest request);

    // ── Browse / list ─────────────────────────────────────────────────────────

    PagedResponse<PlaceSummaryResponse> listPlaces(int page, int size);

    PagedResponse<PlaceSummaryResponse> listByCategory(PlaceCategory category, int page, int size);

    PagedResponse<PlaceSummaryResponse> listByUser(UUID userId, int page, int size);

    List<PlaceSummaryResponse> getTopRatedPlaces();

    // ── Seeding from external sources ─────────────────────────────────────────

    int seedFromGooglePlaces(double lat, double lng, int radiusMeters,
                             String type, UUID seedUserId);

    // ── Internal / Kafka-driven ───────────────────────────────────────────────

    void updateSafetyScore(UUID placeId, BigDecimal newScore, int totalRatings);

    // ── Moderation ────────────────────────────────────────────────────────────

    void reportPlace(UUID id, UUID callerUserId);

    PlaceResponse verifyPlace(UUID id);

    void adminDeletePlace(UUID id);
}
