package com.user.UserMicroservice.externalservices;

import com.user.UserMicroservice.entity.Rating;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name="RATINGMICROSERVICE")
public interface RatingService {

    // create
    @PostMapping("/ratings")
    public Rating createRating(Rating rating);

    // read
    @GetMapping("/ratings")
    public List<Rating> getRatings();

    @GetMapping("/ratings/{userId}")
    public List<Rating> getRatingsByUserId(@PathVariable String userId);

    @GetMapping("/ratings/{placeId}")
    public List<Rating> getRatingsByPlaceId(@PathVariable String placeId);

    @GetMapping("/places/{placeId}/period")
    public ResponseEntity<List<Rating>> getPlaceRatingsByPeriod(
            @PathVariable String placeId,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    );

    @GetMapping("/users/{userId}/period")
    public ResponseEntity<List<Rating>> getUserRatingsByPeriod(
            @PathVariable String userId,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    );

    @GetMapping("/ratings/places/{placeId}/average")
    Double getAverageRatingByPlaceId(@PathVariable("placeId") String placeId);

//    // update
//    @PutMapping("/ratings/{ratingId}")
//    public Rating updateRating(@PathVariable String ratingId, Rating rating);
//
//    // delete
//    @DeleteMapping("/ratings/{ratingId")
//    public void deleteRating(@PathVariable String ratingId);
}
