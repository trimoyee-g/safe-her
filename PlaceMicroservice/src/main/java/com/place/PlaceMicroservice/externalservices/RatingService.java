package com.place.PlaceMicroservice.externalservices;


import com.place.PlaceMicroservice.entity.Rating;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name="RATINGMICROSERVICE")
public interface RatingService {

    @PostMapping("/ratings")
    public Rating createRating(Rating rating);

    @GetMapping("/ratings")
    public List<Rating> getRatings();

    @GetMapping("/ratings/users/{userId}")
    public List<Rating> getRatingsByUserId(@PathVariable String userId);

    @GetMapping("/ratings/places/{placeId}")
    public List<Rating> getRatingsByPlaceId(@PathVariable String placeId);

    @GetMapping("/ratings/places/{placeId}/average")
    Double getAverageRatingByPlaceId(@PathVariable("placeId") String placeId);

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

//    @PutMapping("/ratings/{ratingId}")
//    public Rating updateRating(@PathVariable String ratingId, Rating rating);

//    @DeleteMapping("/ratings/{ratingId")
//    public void deleteRating(@PathVariable String ratingId);
}

