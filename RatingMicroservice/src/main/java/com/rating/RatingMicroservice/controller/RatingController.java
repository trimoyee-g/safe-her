package com.rating.RatingMicroservice.controller;

import com.rating.RatingMicroservice.entity.Rating;
import com.rating.RatingMicroservice.service.RatingService;
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
    public ResponseEntity<Rating> create(@RequestBody Rating rating){
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.create(rating));
    }

    @GetMapping
    public ResponseEntity<List<Rating>> getRatings(){
        return ResponseEntity.ok(ratingService.getAllRatings());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Rating>> getRatingsByUserId(@PathVariable String userId){
        return ResponseEntity.ok((ratingService.getRatingByUserId(userId)));
    }

    @GetMapping("/places/{placeId}")
    public ResponseEntity<List<Rating>> getRatingsByPlaceId(@PathVariable String placeId){
        return ResponseEntity.ok((ratingService.getRatingByPlaceId(placeId)));
    }
}
