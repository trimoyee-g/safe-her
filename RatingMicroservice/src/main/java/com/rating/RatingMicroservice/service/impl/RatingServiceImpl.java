package com.rating.RatingMicroservice.service.impl;

import com.rating.RatingMicroservice.entity.Rating;
import com.rating.RatingMicroservice.repository.RatingRepository;
import com.rating.RatingMicroservice.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingServiceImpl implements RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    // create
    @Override
    public Rating create(Rating rating) {
        return ratingRepository.save(rating);
    }

    // read
    @Override
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }

    @Override
    public List<Rating> getRatingByUserId(String userId) {
        return ratingRepository.findByUserId(userId);
    }

    @Override
    public List<Rating> getRatingByPlaceId(String placeId) {
        return ratingRepository.findByPlaceId(placeId);
    }
}
