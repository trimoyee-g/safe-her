package com.safeher.ratingservice.repository.jpa;

import com.safeher.ratingservice.entity.jpa.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {

    Optional<Rating> findByPlaceIdAndUserId(UUID placeId, UUID userId);

    boolean existsByPlaceIdAndUserIdAndActiveTrue(UUID placeId, UUID userId);

    Page<Rating> findByPlaceIdAndActiveTrue(UUID placeId, Pageable pageable);

    Page<Rating> findByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    long countByPlaceIdAndActiveTrue(UUID placeId);

    Page<Rating> findByFlaggedTrueAndSuppressedFalseAndActiveTrue(Pageable pageable);

    // ── Aggregation – compute avg score and total ratings per place ────────────

    @Aggregation(pipeline = {
        "{ $match: { 'place_id': ?0, 'active': true } }",
        "{ $group: { _id: '$place_id', avgScore: { $avg: '$score' }, totalRatings: { $sum: 1 } } }"
    })
    Optional<ScoreAggregation> computeAggregation(UUID placeId);

    // ── Score distribution (for the ratings breakdown widget) ─────────────────

    @Aggregation(pipeline = {
        "{ $match: { 'place_id': ?0, 'active': true } }",
        "{ $group: { _id: '$score', count: { $sum: 1 } } }",
        "{ $sort: { '_id': 1 } }"
    })
    java.util.List<ScoreDistribution> computeDistribution(UUID placeId);

    // ── Interfaces for aggregation projections ─────────────────────────────────

    interface ScoreAggregation {
        double getAvgScore();
        int getTotalRatings();
    }

    interface ScoreDistribution {
        int get_id();     // the score value (1-5)
        int getCount();
    }
}
