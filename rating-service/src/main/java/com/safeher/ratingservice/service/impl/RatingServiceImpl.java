package com.safeher.ratingservice.service.impl;

import com.safeher.ratingservice.client.PlaceServiceClient;
import com.safeher.ratingservice.client.UserServiceClient;
import com.safeher.ratingservice.dto.request.CreateRatingRequest;
import com.safeher.ratingservice.dto.request.KeywordSearchRequest;
import com.safeher.ratingservice.dto.request.UpdateRatingRequest;
import com.safeher.ratingservice.dto.response.PagedResponse;
import com.safeher.ratingservice.dto.response.PlaceRatingSummary;
import com.safeher.ratingservice.dto.response.RatingResponse;
import com.safeher.ratingservice.entity.document.RatingDocument;
import com.safeher.ratingservice.entity.jpa.Rating;
import com.safeher.ratingservice.enums.RatingSortBy;
import com.safeher.ratingservice.event.RatingCreatedEvent;
import com.safeher.ratingservice.event.RatingDeletedEvent;
import com.safeher.ratingservice.event.RatingUpdatedEvent;
import com.safeher.ratingservice.event.publisher.RatingEventPublisher;
import com.safeher.ratingservice.exception.DuplicateRatingException;
import com.safeher.ratingservice.exception.ForbiddenException;
import com.safeher.ratingservice.exception.RatingNotFoundException;
import com.safeher.ratingservice.mapper.RatingMapper;
import com.safeher.ratingservice.repository.elasticsearch.RatingSearchRepository;
import com.safeher.ratingservice.repository.jpa.RatingRepository;
import com.safeher.ratingservice.service.RatingSearchService;
import com.safeher.ratingservice.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository       ratingRepository;
    private final RatingSearchRepository searchRepository;
    private final RatingSearchService    ratingSearchService;
    private final RatingMapper           ratingMapper;
    private final RatingEventPublisher   eventPublisher;
    private final PlaceServiceClient     placeServiceClient;
    private final UserServiceClient      userServiceClient;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public RatingResponse createRating(CreateRatingRequest request, UUID callerUserId) {
        if (ratingRepository.existsByPlaceIdAndUserIdAndActiveTrue(request.getPlaceId(), callerUserId)) {
            throw new DuplicateRatingException(
                    "You have already reviewed this place. Use the update endpoint to edit your review.");
        }

        // Fetch and denormalise author info (fallback-safe)
        UserServiceClient.UserInfo userInfo = userServiceClient.getPublicProfile(callerUserId);

        Rating rating = ratingMapper.toEntity(request);
        rating.setUserId(callerUserId);
        rating.setAuthorDisplayName(userInfo.displayName());
        rating.setAuthorAvatarUrl(userInfo.avatarUrl());

        Rating saved = ratingRepository.save(rating);
        log.info("Created rating [id={}] for place [{}] by user [{}] score={}",
                saved.getId(), saved.getPlaceId(), callerUserId, saved.getScore());

        // Async: index in ES, increment user review count
        indexAsync(saved);
        userServiceClient.incrementReviewCount(callerUserId);

        // Recompute and publish aggregated score
        publishScoreEvent(saved.getPlaceId(), saved.getId(), saved.getScore(), false, false);

        return ratingMapper.toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public RatingResponse getRatingById(String id, UUID callerUserId, boolean isAdmin) {
        Rating rating = findActiveRating(id);
        return buildResponse(rating, callerUserId, isAdmin);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public RatingResponse updateRating(String id, UpdateRatingRequest request,
                                        UUID callerUserId, boolean isAdmin) {
        Rating rating = findActiveRating(id);
        assertOwnerOrAdmin(rating.getUserId(), callerUserId, isAdmin, "update this review");

        int oldScore = rating.getScore();
        ratingMapper.updateEntity(request, rating);
        Rating saved = ratingRepository.save(rating);

        indexAsync(saved);

        if (oldScore != saved.getScore()) {
            publishScoreEvent(saved.getPlaceId(), saved.getId(), saved.getScore(), false, false);
        }

        log.info("Updated rating [id={}]", id);
        return buildResponse(saved, callerUserId, isAdmin);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteRating(String id, UUID callerUserId, boolean isAdmin) {
        Rating rating = findActiveRating(id);
        assertOwnerOrAdmin(rating.getUserId(), callerUserId, isAdmin, "delete this review");

        rating.setActive(false);
        ratingRepository.save(rating);
        searchRepository.deleteById(id);

        publishScoreEvent(rating.getPlaceId(), id, rating.getScore(), true, false);
        log.info("Soft-deleted rating [id={}]", id);
    }

    // ── Listing ───────────────────────────────────────────────────────────────

    @Override
    public PagedResponse<RatingResponse> getRatingsByPlace(UUID placeId, RatingSortBy sortBy,
                                                            int page, int size,
                                                            UUID callerUserId, boolean isAdmin) {
        Sort sort = switch (sortBy) {
            case OLDEST        -> Sort.by("createdAt").ascending();
            case HIGHEST_SCORE -> Sort.by("score").descending();
            case LOWEST_SCORE  -> Sort.by("score").ascending();
            case MOST_HELPFUL  -> Sort.by("helpfulCount").descending();
            default            -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Rating> result = ratingRepository.findByPlaceIdAndActiveTrue(placeId, pageable);

        List<RatingResponse> content = result.getContent().stream()
                .map(r -> buildResponse(r, callerUserId, isAdmin))
                .toList();

        return toPagedResponse(content, result);
    }

    @Override
    public PagedResponse<RatingResponse> getRatingsByUser(UUID userId, int page, int size,
                                                           UUID callerUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rating> result = ratingRepository.findByUserIdAndActiveTrue(userId, pageable);

        List<RatingResponse> content = result.getContent().stream()
                .map(r -> buildResponse(r, callerUserId, isAdmin))
                .toList();

        return toPagedResponse(content, result);
    }

    // ── Keyword search ────────────────────────────────────────────────────────

    @Override
    public PagedResponse<RatingResponse> keywordSearch(KeywordSearchRequest request,
                                                        UUID callerUserId, boolean isAdmin) {
        SearchHits<RatingDocument> hits = ratingSearchService.search(request);

        List<RatingResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> ratingRepository.findById(doc.getId()).orElse(null))
                .filter(r -> r != null && r.isActive())
                .map(r -> buildResponse(r, callerUserId, isAdmin))
                .toList();

        long total = hits.getTotalHits();
        int  totalPages = (int) Math.ceil((double) total / request.getSize());

        return PagedResponse.<RatingResponse>builder()
                .content(content)
                .page(request.getPage())
                .size(request.getSize())
                .totalElements(total)
                .totalPages(totalPages)
                .last(request.getPage() >= totalPages - 1)
                .build();
    }

    // ── Aggregation ───────────────────────────────────────────────────────────

    @Override
    public PlaceRatingSummary getSummaryForPlace(UUID placeId) {
        var aggOpt = ratingRepository.computeAggregation(placeId);
        var distList = ratingRepository.computeDistribution(placeId);

        Map<Integer, Integer> dist = distList.stream()
                .collect(Collectors.toMap(
                        RatingRepository.ScoreDistribution::get_id,
                        RatingRepository.ScoreDistribution::getCount));

        BigDecimal avg = aggOpt.map(a -> BigDecimal.valueOf(a.getAvgScore())
                .setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);

        int total = aggOpt.map(RatingRepository.ScoreAggregation::getTotalRatings).orElse(0);

        return PlaceRatingSummary.builder()
                .placeId(placeId)
                .averageScore(avg)
                .totalRatings(total)
                .scoreDistribution(dist)
                .build();
    }

    @Override
    public BigDecimal getAggregatedScore(UUID placeId) {
        return ratingRepository.computeAggregation(placeId)
                .map(a -> BigDecimal.valueOf(a.getAvgScore()).setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    // ── AI Moderation ─────────────────────────────────────────────────────────

    @Override
    public void suppress(String ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found: " + ratingId));
        rating.setActive(false);
        rating.setSuppressed(true);
        ratingRepository.save(rating);
        searchRepository.deleteById(ratingId);
        log.info("Rating [{}] auto-suppressed by AI moderation", ratingId);
    }

    @Override
    public void flag(String ratingId, String reason, double confidence) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found: " + ratingId));
        rating.setFlagged(true);
        rating.setFlagReason(reason);
        rating.setFlagConfidence(confidence);
        ratingRepository.save(rating);
        log.info("Rating [{}] flagged for review: reason={} confidence={}", ratingId, reason, confidence);
    }

    @Override
    public PagedResponse<RatingResponse> getFlaggedReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rating> result = ratingRepository.findByFlaggedTrueAndSuppressedFalseAndActiveTrue(pageable);
        List<RatingResponse> content = result.getContent().stream()
                .map(ratingMapper::toResponse)
                .toList();
        return toPagedResponse(content, result);
    }

    // ── Engagement ────────────────────────────────────────────────────────────

    @Override
    public void markHelpful(String ratingId, UUID callerUserId) {
        Rating rating = findActiveRating(ratingId);

        if (rating.getUserId().equals(callerUserId)) {
            throw new ForbiddenException("You cannot mark your own review as helpful");
        }

        String voterKey = callerUserId.toString();
        if (rating.getHelpfulVoters().contains(voterKey)) {
            // Toggle off
            rating.getHelpfulVoters().remove(voterKey);
            rating.setHelpfulCount(Math.max(0, rating.getHelpfulCount() - 1));
        } else {
            rating.getHelpfulVoters().add(voterKey);
            rating.setHelpfulCount(rating.getHelpfulCount() + 1);
        }

        ratingRepository.save(rating);
        indexAsync(rating);
    }

    @Override
    public void reportRating(String ratingId, UUID callerUserId) {
        Rating rating = findActiveRating(ratingId);
        String reporter = callerUserId.toString();

        if (!rating.getReportedBy().contains(reporter)) {
            rating.getReportedBy().add(reporter);
            rating.setReportedCount(rating.getReportedCount() + 1);
            ratingRepository.save(rating);
            log.info("Rating [{}] reported by user [{}] (total reports: {})",
                    ratingId, callerUserId, rating.getReportedCount());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Rating findActiveRating(String id) {
        return ratingRepository.findById(id)
                .filter(Rating::isActive)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found: " + id));
    }

    private void assertOwnerOrAdmin(UUID ownerId, UUID callerId, boolean isAdmin, String action) {
        if (!isAdmin && !ownerId.equals(callerId)) {
            throw new ForbiddenException("You are not authorised to " + action);
        }
    }

    /**
     * Builds the appropriate response DTO depending on whether the caller
     * is the author, an admin, or a regular third-party viewer.
     */
    private RatingResponse buildResponse(Rating rating, UUID callerUserId, boolean isAdmin) {
        boolean isOwner = callerUserId != null && callerUserId.equals(rating.getUserId());
        RatingResponse resp = (isOwner || isAdmin)
                ? ratingMapper.toResponse(rating)
                : ratingMapper.toPublicResponse(rating);

        // Populate markedHelpfulByMe
        if (callerUserId != null) {
            resp.setMarkedHelpfulByMe(rating.getHelpfulVoters().contains(callerUserId.toString()));
        }
        return resp;
    }

    @Async
    void indexAsync(Rating rating) {
        try {
            searchRepository.save(ratingMapper.toDocument(rating));
        } catch (Exception ex) {
            log.error("ES index failed for rating [{}]: {}", rating.getId(), ex.getMessage());
        }
    }

    private void publishScoreEvent(UUID placeId, String ratingId, int score,
                                   boolean isDelete, boolean isUpdate) {
        var aggOpt = ratingRepository.computeAggregation(placeId);
        double avg   = aggOpt.map(RatingRepository.ScoreAggregation::getAvgScore).orElse(0.0);
        int    total = aggOpt.map(RatingRepository.ScoreAggregation::getTotalRatings).orElse(0);

        if (isDelete) {
            eventPublisher.publishDeleted(RatingDeletedEvent.builder()
                    .ratingId(ratingId).placeId(placeId)
                    .newAverageScore(avg).totalRatings(total).build());
        } else if (isUpdate) {
            eventPublisher.publishUpdated(RatingUpdatedEvent.builder()
                    .ratingId(ratingId).placeId(placeId).newScore(score)
                    .newAverageScore(avg).totalRatings(total).build());
        } else {
            eventPublisher.publishCreated(RatingCreatedEvent.builder()
                    .ratingId(ratingId).placeId(placeId).userId(null).score(score)
                    .newAverageScore(avg).totalRatings(total).build());
        }
    }

    private <T> PagedResponse<T> toPagedResponse(List<T> content, Page<?> page) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
