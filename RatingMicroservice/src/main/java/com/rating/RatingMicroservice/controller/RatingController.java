package com.rating.RatingMicroservice.controller;

import com.rating.RatingMicroservice.entity.Rating;
import com.rating.RatingMicroservice.service.RatingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping
    public ResponseEntity<Rating> createRating(@RequestBody Rating rating){
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.create(rating));
    }

    @GetMapping
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "userPlaceFallback")
    @Retry(name = "userPlaceRetry", fallbackMethod = "userPlaceFallback")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "userPlaceFallback")
    public ResponseEntity<List<Rating>> getRatings(){
        return ResponseEntity.ok(ratingService.getAllRatings());
    }

    @GetMapping("/users/{userId}")
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "userPlaceFallback")
    @Retry(name = "userPlaceRetry", fallbackMethod = "userPlaceFallback")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "userPlaceFallback")
    public ResponseEntity<List<Rating>> getRatingsByUserId(@PathVariable String userId){
        return ResponseEntity.ok((ratingService.getRatingByUserId(userId)));
    }

    @GetMapping("/places/{placeId}")
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "userPlaceFallback")
    @Retry(name = "userPlaceRetry", fallbackMethod = "userPlaceFallback")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "userPlaceFallback")
    public ResponseEntity<List<Rating>> getRatingsByPlaceId(@PathVariable String placeId){
        return ResponseEntity.ok((ratingService.getRatingByPlaceId(placeId)));
    }

    public ResponseEntity<Rating> ratingPlaceFallback(String userId, Exception ex){
        Rating rating = Rating.builder()
                .ratingId("000")
                .placeId("000")
                .userId("000")
                .rating(-100)
                .feedback("too many requests or some service is down")
                .build();
        return new ResponseEntity<>(rating, HttpStatus.TOO_MANY_REQUESTS);
    }
}
