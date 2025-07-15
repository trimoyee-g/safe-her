package com.rating.RatingMicroservice.service;


import com.rating.RatingMicroservice.entity.Rating;

import java.time.LocalDateTime;
import java.util.*;

public interface RatingService {

    // create
    Rating create(Rating rating);

    // read
    List<Rating> getAllRatings();
    List<Rating> getRatingByUserId(String userId);
    List<Rating> getRatingByPlaceId(String placeId);
    List<Rating> getRatingsByUserIdWithinPeriod(String userId, LocalDateTime start, LocalDateTime end);
    List<Rating> getRatingsByPlaceIdWithinPeriod(String placeId, LocalDateTime start, LocalDateTime end);
    Double getAverageRatingByPlaceId(String placeId);



}
