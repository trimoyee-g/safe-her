package com.rating.RatingMicroservice.repository;

import com.rating.RatingMicroservice.entity.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;

import java.util.List;

public interface RatingRepository extends MongoRepository<Rating, String> {

    //custom methods
    List<Rating> findByUserId(String userId);
    List<Rating> findByPlaceId(String placeId);
    List<Rating> findByUserIdAndCreatedOnBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    List<Rating> findByPlaceIdAndCreatedOnBetween(String placeId, LocalDateTime startDate, LocalDateTime endDate);
}
