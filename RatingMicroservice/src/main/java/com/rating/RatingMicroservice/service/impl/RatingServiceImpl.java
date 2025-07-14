package com.rating.RatingMicroservice.service.impl;

import com.rating.RatingMicroservice.entity.Rating;
import com.rating.RatingMicroservice.exceptions.ResourceNotFoundException;
import com.rating.RatingMicroservice.repository.RatingRepository;
import com.rating.RatingMicroservice.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingServiceImpl implements RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    // Create
    @Override
    public Rating create(Rating rating) {
        return ratingRepository.save(rating);
    }

    // Get all ratings
    @Override
    public List<Rating> getAllRatings() {
        List<Rating> ratings = ratingRepository.findAll();
        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found in the system.");
        }
        return ratings;
    }

    // Get by userId
    @Override
    public List<Rating> getRatingByUserId(String userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);
        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found for user ID: " + userId);
        }
        return ratings;
    }

    // Get by placeId
    @Override
    public List<Rating> getRatingByPlaceId(String placeId) {
        List<Rating> ratings = ratingRepository.findByPlaceId(placeId);
        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found for place ID: " + placeId);
        }
        return ratings;
    }
}
