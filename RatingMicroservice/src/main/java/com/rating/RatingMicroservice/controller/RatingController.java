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

import java.time.LocalDateTime;
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

    @GetMapping("/users/{userId}/period")
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "userPlaceFallback")
    @Retry(name = "userPlaceRetry", fallbackMethod = "userPlaceFallback")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "userPlaceFallback")
    public ResponseEntity<List<Rating>> getUserRatingsByPeriod(
            @PathVariable String userId,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    ) {
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        return ResponseEntity.ok(ratingService.getRatingsByUserIdWithinPeriod(userId, startDate, endDate));
    }

    @GetMapping("/places/{placeId}/period")
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "userPlaceFallback")
    @Retry(name = "userPlaceRetry", fallbackMethod = "userPlaceFallback")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "userPlaceFallback")
    public ResponseEntity<List<Rating>> getPlaceRatingsByPeriod(
            @PathVariable String placeId,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    ) {
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        return ResponseEntity.ok(ratingService.getRatingsByPlaceIdWithinPeriod(placeId, startDate, endDate));
    }

    public ResponseEntity<List<Rating>> userPlaceFallback(String userId, Throwable ex) {
        Rating dummyRating = Rating.builder()
                .ratingId("000")
                .userId("000")
                .placeId("000")
                .rating(-1)
                .feedback("Fallback: Service unavailable")
                .build();
        return new ResponseEntity<>(List.of(dummyRating), HttpStatus.TOO_MANY_REQUESTS);
    }

    @GetMapping("/places/{placeId}/average")
    @CircuitBreaker(name="userPlaceBreaker", fallbackMethod = "ratingPlaceFallbackAvg")
    @Retry(name = "userPlaceRetry", fallbackMethod = "ratingPlaceFallbackAvg")
    @RateLimiter(name = "userPlaceRateLimiter", fallbackMethod = "ratingPlaceFallbackAvg")
    public ResponseEntity<Double> getAverageRating(@PathVariable String placeId) {
        return ResponseEntity.ok(ratingService.getAverageRatingByPlaceId(placeId));
    }

    // Fallback
    public ResponseEntity<Double> ratingPlaceFallbackAvg(String placeId, Exception ex) {
        return ResponseEntity.ok(0.0); // default fallback value
    }

}
