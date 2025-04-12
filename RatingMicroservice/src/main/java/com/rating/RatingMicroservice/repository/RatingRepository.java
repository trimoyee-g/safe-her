package com.rating.RatingMicroservice.repository;

import com.rating.RatingMicroservice.entity.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RatingRepository extends MongoRepository<Rating, String> {

    //custom methods
    List<Rating> findByUserId(String userId);
    List<Rating> findByPlaceId(String placeId);
}
