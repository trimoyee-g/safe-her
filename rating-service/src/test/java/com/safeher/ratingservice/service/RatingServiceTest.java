package com.safeher.ratingservice.service;

import com.safeher.ratingservice.client.PlaceServiceClient;
import com.safeher.ratingservice.client.UserServiceClient;
import com.safeher.ratingservice.dto.request.CreateRatingRequest;
import com.safeher.ratingservice.dto.response.RatingResponse;
import com.safeher.ratingservice.entity.jpa.Rating;
import com.safeher.ratingservice.event.publisher.RatingEventPublisher;
import com.safeher.ratingservice.exception.DuplicateRatingException;
import com.safeher.ratingservice.exception.ForbiddenException;
import com.safeher.ratingservice.exception.RatingNotFoundException;
import com.safeher.ratingservice.mapper.RatingMapper;
import com.safeher.ratingservice.repository.elasticsearch.RatingSearchRepository;
import com.safeher.ratingservice.repository.jpa.RatingRepository;
import com.safeher.ratingservice.service.impl.RatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock RatingRepository       ratingRepository;
    @Mock RatingSearchRepository searchRepository;
    @Mock RatingSearchService    ratingSearchService;
    @Mock RatingMapper           ratingMapper;
    @Mock RatingEventPublisher   eventPublisher;
    @Mock PlaceServiceClient     placeServiceClient;
    @Mock UserServiceClient      userServiceClient;

    @InjectMocks RatingServiceImpl ratingService;

    private UUID userId;
    private UUID placeId;
    private Rating sampleRating;
    private RatingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId  = UUID.randomUUID();
        placeId = UUID.randomUUID();

        sampleRating = Rating.builder()
                .id("mongo-id-1")
                .placeId(placeId)
                .userId(userId)
                .score(4)
                .body("Very well lit street, felt safe at night.")
                .anonymous(false)
                .active(true)
                .build();

        sampleResponse = RatingResponse.builder()
                .id("mongo-id-1")
                .placeId(placeId)
                .score(4)
                .body("Very well lit street, felt safe at night.")
                .build();
    }

    // ── createRating ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRating – success publishes event and indexes in ES")
    void createRating_success() {
        CreateRatingRequest req = new CreateRatingRequest();
        req.setPlaceId(placeId);
        req.setScore(4);
        req.setBody("Very well lit street.");

        when(ratingRepository.existsByPlaceIdAndUserIdAndActiveTrue(placeId, userId)).thenReturn(false);
        when(userServiceClient.getPublicProfile(userId))
                .thenReturn(new UserServiceClient.UserInfo(userId, "testuser", "Test User", null));
        when(ratingMapper.toEntity(req)).thenReturn(sampleRating);
        when(ratingRepository.save(sampleRating)).thenReturn(sampleRating);
        when(ratingMapper.toResponse(sampleRating)).thenReturn(sampleResponse);
        when(ratingRepository.computeAggregation(placeId)).thenReturn(Optional.empty());

        RatingResponse result = ratingService.createRating(req, userId);

        assertThat(result.getScore()).isEqualTo(4);
        verify(eventPublisher).publishCreated(any());
    }

    @Test
    @DisplayName("createRating – duplicate throws DuplicateRatingException")
    void createRating_duplicate_throws() {
        CreateRatingRequest req = new CreateRatingRequest();
        req.setPlaceId(placeId);
        req.setScore(3);

        when(ratingRepository.existsByPlaceIdAndUserIdAndActiveTrue(placeId, userId)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.createRating(req, userId))
                .isInstanceOf(DuplicateRatingException.class);
    }

    // ── getRatingById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRatingById – author gets full response")
    void getRatingById_authorGetsFullResponse() {
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        when(ratingMapper.toResponse(sampleRating)).thenReturn(sampleResponse);

        RatingResponse result = ratingService.getRatingById("mongo-id-1", userId, false);

        assertThat(result).isNotNull();
        verify(ratingMapper).toResponse(sampleRating);
        verify(ratingMapper, never()).toPublicResponse(any());
    }

    @Test
    @DisplayName("getRatingById – third party gets public response")
    void getRatingById_strangerGetsPublicResponse() {
        UUID stranger = UUID.randomUUID();
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        when(ratingMapper.toPublicResponse(sampleRating)).thenReturn(sampleResponse);

        ratingService.getRatingById("mongo-id-1", stranger, false);

        verify(ratingMapper).toPublicResponse(sampleRating);
        verify(ratingMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getRatingById – not found throws RatingNotFoundException")
    void getRatingById_notFound_throws() {
        when(ratingRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.getRatingById("bad-id", userId, false))
                .isInstanceOf(RatingNotFoundException.class);
    }

    // ── deleteRating ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRating – owner can delete own review")
    void deleteRating_ownerCanDelete() {
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        when(ratingRepository.save(any())).thenReturn(sampleRating);
        when(ratingRepository.computeAggregation(placeId)).thenReturn(Optional.empty());

        assertThatCode(() -> ratingService.deleteRating("mongo-id-1", userId, false))
                .doesNotThrowAnyException();
        verify(eventPublisher).publishDeleted(any());
    }

    @Test
    @DisplayName("deleteRating – admin can delete any review")
    void deleteRating_adminCanDelete() {
        UUID adminId = UUID.randomUUID();
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        when(ratingRepository.save(any())).thenReturn(sampleRating);
        when(ratingRepository.computeAggregation(placeId)).thenReturn(Optional.empty());

        assertThatCode(() -> ratingService.deleteRating("mongo-id-1", adminId, true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteRating – stranger throws ForbiddenException")
    void deleteRating_strangerForbidden() {
        UUID stranger = UUID.randomUUID();
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));

        assertThatThrownBy(() -> ratingService.deleteRating("mongo-id-1", stranger, false))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── markHelpful ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("markHelpful – author cannot vote on own review")
    void markHelpful_selfVote_forbidden() {
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));

        assertThatThrownBy(() -> ratingService.markHelpful("mongo-id-1", userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own review");
    }

    @Test
    @DisplayName("markHelpful – toggles vote on/off")
    void markHelpful_toggles() {
        UUID voter = UUID.randomUUID();
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        when(ratingRepository.save(any())).thenReturn(sampleRating);

        ratingService.markHelpful("mongo-id-1", voter);
        assertThat(sampleRating.getHelpfulCount()).isEqualTo(1);
        assertThat(sampleRating.getHelpfulVoters()).contains(voter.toString());

        // Second call toggles off
        when(ratingRepository.findById("mongo-id-1")).thenReturn(Optional.of(sampleRating));
        ratingService.markHelpful("mongo-id-1", voter);
        assertThat(sampleRating.getHelpfulCount()).isEqualTo(0);
    }
}
