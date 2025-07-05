package com.user.UserMicroservice.externalservices;

import com.user.UserMicroservice.entity.Rating;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

//    // update
//    @PutMapping("/ratings/{ratingId}")
//    public Rating updateRating(@PathVariable String ratingId, Rating rating);
//
//    // delete
//    @DeleteMapping("/ratings/{ratingId")
//    public void deleteRating(@PathVariable String ratingId);
}
