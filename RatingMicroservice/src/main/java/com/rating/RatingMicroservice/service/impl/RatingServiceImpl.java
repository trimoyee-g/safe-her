package com.rating.RatingMicroservice.service.impl;

import com.rating.RatingMicroservice.entity.Rating;
import com.rating.RatingMicroservice.exceptions.ResourceNotFoundException;
import com.rating.RatingMicroservice.repository.RatingRepository;
import com.rating.RatingMicroservice.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RatingServiceImpl implements RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    // Create
    @Override
    public Rating create(Rating rating) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        rating.setCreatedOn(LocalDateTime.parse(timestamp));
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

    @Override
    public List<Rating> getRatingsByUserIdWithinPeriod(String userId, LocalDateTime start, LocalDateTime end) {
        List<Rating> ratings = ratingRepository.findByUserIdAndCreatedOnBetween(userId, start, end);
        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found for user " + userId + " between " + start + " and " + end);
        }
        return ratings;
    }

    @Override
    public List<Rating> getRatingsByPlaceIdWithinPeriod(String placeId, LocalDateTime start, LocalDateTime end) {
        List<Rating> ratings = ratingRepository.findByPlaceIdAndCreatedOnBetween(placeId, start, end);
        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found for place " + placeId + " between " + start + " and " + end);
        }
        return ratings;
    }

    @Override
    public Double getAverageRatingByPlaceId(String placeId) {
        List<Rating> ratings = ratingRepository.findByPlaceId(placeId);

        if (ratings.isEmpty()) {
            throw new ResourceNotFoundException("No ratings found for place ID: " + placeId);
        }

        double average = ratings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);

        return average;
    }

}
