package com.place.PlaceMicroservice.externalservices;


import com.place.PlaceMicroservice.entity.Rating;
import org.springframework.cloud.openfeign.FeignClient;
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

//    @PutMapping("/ratings/{ratingId}")
//    public Rating updateRating(@PathVariable String ratingId, Rating rating);

//    @DeleteMapping("/ratings/{ratingId")
//    public void deleteRating(@PathVariable String ratingId);
}

