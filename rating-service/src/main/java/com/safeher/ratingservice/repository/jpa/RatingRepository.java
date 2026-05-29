package com.safeher.ratingservice.repository.jpa;

import com.safeher.ratingservice.entity.jpa.Rating;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    List<ScoreAggregation> computeAggregation(UUID placeId);

    // ── Score distribution (for the ratings breakdown widget) ─────────────────

    @Aggregation(pipeline = {
        "{ $match: { 'place_id': ?0, 'active': true } }",
        "{ $group: { _id: '$score', count: { $sum: 1 } } }",
        "{ $project: { id: '$_id', count: 1, _id: 0 } }",
        "{ $sort: { 'id': 1 } }"
    })
    java.util.List<ScoreDistribution> computeDistribution(UUID placeId);

    // ── DTOs for aggregation results ───────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    class ScoreAggregation {
        private double avgScore;
        private int    totalRatings;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    class ScoreDistribution {
        private int id;     // score bucket (1-5); renamed from _id via $project in the aggregation above
        private int count;
    }
}
