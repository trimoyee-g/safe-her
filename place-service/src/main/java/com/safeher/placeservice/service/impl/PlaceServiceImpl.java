package com.safeher.placeservice.service.impl;

import com.safeher.placeservice.client.GooglePlacesClient;
import com.safeher.placeservice.client.UserServiceClient;
import com.safeher.placeservice.dto.request.CreatePlaceRequest;
import com.safeher.placeservice.dto.request.GeoSearchRequest;
import com.safeher.placeservice.dto.request.KeywordSearchRequest;
import com.safeher.placeservice.dto.request.UpdatePlaceRequest;
import com.safeher.placeservice.dto.response.PagedResponse;
import com.safeher.placeservice.dto.response.PlaceResponse;
import com.safeher.placeservice.dto.response.PlaceSummaryResponse;
import com.safeher.placeservice.entity.Place;
import com.safeher.placeservice.enums.PlaceCategory;
import com.safeher.placeservice.enums.PlaceSource;
import com.safeher.placeservice.event.PlaceCreatedEvent;
import com.safeher.placeservice.event.PlaceDeletedEvent;
import com.safeher.placeservice.event.PlaceScoreUpdatedEvent;
import com.safeher.placeservice.event.PlaceUpdatedEvent;
import com.safeher.placeservice.event.publisher.PlaceEventPublisher;
import com.safeher.placeservice.exception.ForbiddenException;
import com.safeher.placeservice.exception.PlaceNotFoundException;
import com.safeher.placeservice.mapper.PlaceMapper;
import com.safeher.placeservice.repository.elasticsearch.PlaceSearchRepository;
import com.safeher.placeservice.repository.jpa.PlaceRepository;
import com.safeher.placeservice.service.PlaceSearchService;
import com.safeher.placeservice.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlaceServiceImpl implements PlaceService {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository          placeRepository;
    private final PlaceSearchRepository    searchRepository;
    private final PlaceSearchService       placeSearchService;
    private final PlaceMapper              placeMapper;
    private final PlaceEventPublisher      eventPublisher;
    private final GooglePlacesClient       googlePlacesClient;
    private final UserServiceClient        userServiceClient;

    @Value("${app.geo.max-search-radius-meters:50000}")
    private double maxRadiusMeters;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaceResponse createPlace(CreatePlaceRequest request, UUID callerUserId) {
        Place place = placeMapper.toEntity(request);
        place.setSource(PlaceSource.USER);
        place.setCreatedBy(callerUserId);

        Place saved = placeRepository.save(place);
        log.info("Created place [id={}] '{}' by user [{}]", saved.getId(), saved.getName(), callerUserId);

        // Index in Elasticsearch asynchronously
        indexAsync(saved);

        eventPublisher.publishPlaceCreated(PlaceCreatedEvent.builder()
                .placeId(saved.getId())
                .name(saved.getName())
                .category(saved.getCategory().name())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .city(saved.getCity())
                .country(saved.getCountry())
                .createdBy(callerUserId)
                .build());

        return placeMapper.toResponse(saved);
    }

    @Override
    public PlaceResponse getPlaceById(UUID id) {
        Place place = findActivePlace(id);
        return placeMapper.toResponse(place);
    }

    @Override
    @Transactional
    public PlaceResponse updatePlace(UUID id, UpdatePlaceRequest request,
                                     UUID callerUserId, boolean isAdmin) {
        Place place = findActivePlace(id);
        assertOwnerOrAdmin(place.getCreatedBy(), callerUserId, isAdmin, "update this place");

        placeMapper.updateEntity(request, place);
        Place saved = placeRepository.save(place);

        indexAsync(saved);

        eventPublisher.publishPlaceUpdated(PlaceUpdatedEvent.builder()
                .placeId(saved.getId())
                .name(saved.getName())
                .category(saved.getCategory().name())
                .build());

        return placeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePlace(UUID id, UUID callerUserId, boolean isAdmin) {
        Place place = findActivePlace(id);
        assertOwnerOrAdmin(place.getCreatedBy(), callerUserId, isAdmin, "delete this place");
        softDelete(place);
    }

    // ── Geo-search ────────────────────────────────────────────────────────────

    @Override
    public PagedResponse<PlaceSummaryResponse> geoSearch(GeoSearchRequest request) {
        double radius = Math.min(request.getRadiusMeters(), maxRadiusMeters);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<Place> page;
        if (request.getCategory() != null) {
            page = placeRepository.findWithinRadiusByCategory(
                    request.getLatitude(), request.getLongitude(), radius,
                    request.getCategory().name(), pageable);
        } else {
            page = placeRepository.findWithinRadius(
                    request.getLatitude(), request.getLongitude(), radius, pageable);
        }

        List<PlaceSummaryResponse> content = page.getContent().stream()
                .map(p -> {
                    PlaceSummaryResponse s = placeMapper.toSummary(p);
                    s.setDistanceMeters(haversineMeters(
                            request.getLatitude(), request.getLongitude(),
                            p.getLatitude(), p.getLongitude()));
                    return s;
                })
                .toList();

        return PagedResponse.<PlaceSummaryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ── Keyword search ────────────────────────────────────────────────────────

    @Override
    public PagedResponse<PlaceSummaryResponse> keywordSearch(KeywordSearchRequest request) {
        return placeSearchService.search(request);
    }

    // ── Browse / list ─────────────────────────────────────────────────────────

    @Override
    public PagedResponse<PlaceSummaryResponse> listPlaces(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toPagedSummary(placeRepository.findAllByActiveTrue(pageable));
    }

    @Override
    public PagedResponse<PlaceSummaryResponse> listByCategory(PlaceCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("safetyScore").descending());
        return toPagedSummary(placeRepository.findAllByActiveTrueAndCategory(category, pageable));
    }

    @Override
    public PagedResponse<PlaceSummaryResponse> listByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toPagedSummary(placeRepository.findAllByCreatedByAndActiveTrue(userId, pageable));
    }

    @Override
    public List<PlaceSummaryResponse> getTopRatedPlaces() {
        return placeMapper.toSummaryList(placeRepository.findTop10ByActiveTrueOrderBySafetyScoreDesc());
    }

    // ── Seeding ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public int seedFromGooglePlaces(double lat, double lng, int radiusMeters,
                                    String type, UUID seedUserId) {
        List<GooglePlacesClient.GooglePlaceResult> results =
                googlePlacesClient.nearbySearch(lat, lng, radiusMeters, type);

        int seeded = 0;
        for (GooglePlacesClient.GooglePlaceResult r : results) {
            if (placeRepository.existsByExternalIdAndSource(r.getPlaceId(), PlaceSource.GOOGLE.name())) {
                continue;
            }
            try {
                Place place = Place.builder()
                        .externalId(r.getPlaceId())
                        .source(PlaceSource.GOOGLE)
                        .name(r.getName())
                        .address(r.getVicinity())
                        .category(mapGoogleType(r.getTypes()))
                        .location(GEO.createPoint(new Coordinate(
                                r.getGeometry().getLocation().getLng(),
                                r.getGeometry().getLocation().getLat())))
                        .createdBy(seedUserId)
                        .build();

                Place saved = placeRepository.save(place);
                indexAsync(saved);
                seeded++;
            } catch (Exception ex) {
                log.warn("Failed to seed place [{}]: {}", r.getName(), ex.getMessage());
            }
        }
        log.info("Seeded {} places from Google Places (type={}, lat={}, lng={})", seeded, type, lat, lng);
        return seeded;
    }

    // ── Internal / Kafka-driven ───────────────────────────────────────────────

    @Override
    @Transactional
    public void updateDescription(UUID placeId, String description) {
        placeRepository.updateDescription(placeId, description);
        placeRepository.findActiveById(placeId).ifPresent(p -> {
            p.setDescription(description);
            indexAsync(p);
        });
        log.debug("Updated description for place [{}]", placeId);
    }

    @Override
    @Transactional
    public void updateAiSummary(UUID placeId, String summary, OffsetDateTime generatedAt) {
        placeRepository.updateAiSummary(placeId, summary, generatedAt);
        placeRepository.findActiveById(placeId).ifPresent(p -> {
            p.setAiSummary(summary);
            p.setAiSummaryGeneratedAt(generatedAt);
            indexAsync(p);
        });
        log.debug("Updated AI summary for place [{}]", placeId);
    }

    @Override
    @Transactional
    public void updateSafetyScore(UUID placeId, BigDecimal newScore, int totalRatings) {
        placeRepository.updateSafetyScore(placeId, newScore, totalRatings);

        // Re-index in ES
        placeRepository.findActiveById(placeId).ifPresent(p -> {
            p.setSafetyScore(newScore);
            p.setTotalRatings(totalRatings);
            indexAsync(p);
        });

        eventPublisher.publishScoreUpdated(PlaceScoreUpdatedEvent.builder()
                .placeId(placeId)
                .newSafetyScore(newScore)
                .totalRatings(totalRatings)
                .build());

        log.debug("Updated safety score for place [{}] → {}", placeId, newScore);
    }

    // ── Moderation ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void reportPlace(UUID id, UUID callerUserId) {
        findActivePlace(id);   // validates existence
        placeRepository.incrementReportCount(id);
        log.info("Place [{}] reported by user [{}]", id, callerUserId);
    }

    @Override
    @Transactional
    public PlaceResponse verifyPlace(UUID id) {
        Place place = findActivePlace(id);
        place.setVerified(true);
        Place saved = placeRepository.save(place);
        indexAsync(saved);
        return placeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void adminDeletePlace(UUID id) {
        Place place = findActivePlace(id);
        softDelete(place);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Place findActivePlace(UUID id) {
        return placeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new PlaceNotFoundException("Place not found: " + id));
    }

    private void softDelete(Place place) {
        placeRepository.deactivate(place.getId());
        searchRepository.deleteById(place.getId().toString());
        eventPublisher.publishPlaceDeleted(PlaceDeletedEvent.builder()
                .placeId(place.getId()).build());
        log.info("Soft-deleted place [id={}]", place.getId());
    }

    private void assertOwnerOrAdmin(UUID ownerId, UUID callerId, boolean isAdmin, String action) {
        if (!isAdmin && !ownerId.equals(callerId)) {
            throw new ForbiddenException("You are not authorised to " + action);
        }
    }

    @Async
    void indexAsync(Place place) {
        try {
            searchRepository.save(placeMapper.toDocument(place));
            log.debug("ES indexed place [id={}]", place.getId());
        } catch (Exception ex) {
            log.error("ES indexing failed for place [{}]: {}", place.getId(), ex.getMessage());
        }
    }

    private PlaceCategory mapGoogleType(List<String> types) {
        if (types == null || types.isEmpty()) return PlaceCategory.OTHER;
        for (String t : types) {
            return switch (t) {
                case "restaurant", "food" -> PlaceCategory.RESTAURANT;
                case "cafe"              -> PlaceCategory.CAFE;
                case "bar", "night_club" -> PlaceCategory.BAR;
                case "park"              -> PlaceCategory.PARK;
                case "hospital"          -> PlaceCategory.HOSPITAL;
                case "pharmacy"          -> PlaceCategory.PHARMACY;
                case "lodging"           -> PlaceCategory.HOTEL;
                case "shopping_mall"     -> PlaceCategory.SHOPPING_MALL;
                case "transit_station"   -> PlaceCategory.TRANSIT_STATION;
                case "atm"               -> PlaceCategory.ATM;
                case "bank"              -> PlaceCategory.BANK;
                case "school"            -> PlaceCategory.SCHOOL;
                case "university"        -> PlaceCategory.COLLEGE;
                case "gym"               -> PlaceCategory.GYM;
                case "police"            -> PlaceCategory.POLICE_STATION;
                default -> null;
            };
        }
        return PlaceCategory.OTHER;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6_371_000; // Earth radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private PagedResponse<PlaceSummaryResponse> toPagedSummary(Page<Place> page) {
        return PagedResponse.<PlaceSummaryResponse>builder()
                .content(page.getContent().stream().map(placeMapper::toSummary).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
