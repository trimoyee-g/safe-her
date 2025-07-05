package com.rating.RatingMicroservice.service;


import com.rating.RatingMicroservice.entity.Rating;

import java.util.List;

public interface RatingService {

    // create
    Rating create(Rating rating);

    // read
    List<Rating> getAllRatings();
    List<Rating> getRatingByUserId(String userId);
    List<Rating> getRatingByPlaceId(String placeId);

}
