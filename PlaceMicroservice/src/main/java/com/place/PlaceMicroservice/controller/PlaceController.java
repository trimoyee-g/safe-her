package com.place.PlaceMicroservice.controller;

import com.place.PlaceMicroservice.entity.Place;
import com.place.PlaceMicroservice.service.PlaceService;
import com.place.PlaceMicroservice.util.AccessControlUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/places")
public class PlaceController {

    @Autowired
    private PlaceService placeService;

    @PostMapping
    public ResponseEntity<Place> addPlace( @RequestBody Place place,
                                           @RequestHeader("X-User-Id") String requesterId
    ) {
        Place saved = placeService.addPlace(place, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping
    public ResponseEntity<List<Place>> getAllPlaces() {
        List<Place> allPlaces = placeService.getAll();
        return ResponseEntity.ok(allPlaces);
    }

    @GetMapping(value = "/id/{placeId}")
    @CircuitBreaker(name="ratingUserBreakerById", fallbackMethod = "ratingUserFallbackById")
    @Retry(name = "ratingUserRetryById", fallbackMethod = "ratingUserFallbackById")
    @RateLimiter(name = "ratingUserRateLimiterById", fallbackMethod = "ratingUserFallbackById")
    public ResponseEntity<Place> getPlaceByPlaceId(@PathVariable String placeId){
        Place place = placeService.getPlaceByPlaceId(placeId);
        return ResponseEntity.ok(place);
    }
    public ResponseEntity<Place> ratingUserFallbackById(String placeId, Exception ex){
        Place place = Place.builder()
                .placeName("dummy place")
                .placeAbout("too many requests or some service is down")
                .placeLocation("dummy location")
                .placeId("000")
                .build();
        return new ResponseEntity<>(place, HttpStatus.TOO_MANY_REQUESTS);
    }

    @GetMapping(value = "/name/{placeName}")
    @CircuitBreaker(name="ratingUserBreakerByName", fallbackMethod = "ratingUserFallbackByName")
    @Retry(name = "ratingUserRetryByName", fallbackMethod = "ratingUserFallbackByName")
    @RateLimiter(name = "ratingUserRateLimiterByName", fallbackMethod = "ratingUserFallbackByName")
    public ResponseEntity<Place> getPlaceByPlaceName(@PathVariable String placeName){
        Place place = placeService.getPlaceByPlaceName(placeName);
        return ResponseEntity.ok(place);
    }
    public ResponseEntity<Place> ratingUserFallbackByName(String placeName, Exception ex){
        Place place = Place.builder()
                .placeName("dummy place")
                .placeAbout("too many requests or some service is down")
                .placeLocation("dummy location")
                .placeId("000")
                .build();
        return new ResponseEntity<>(place, HttpStatus.TOO_MANY_REQUESTS);
    }

    @GetMapping("/search")
    public ResponseEntity<?> getPlacesByName(@RequestParam("name") String name) {
        List<Place> places = placeService.getPlacesByName(name);
        if (places.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No places found");
        }
        return ResponseEntity.ok(places);
    }

    @PutMapping(value="/id/{placeId}")
    public ResponseEntity<Place> updatePlaceByPlaceId(@PathVariable String placeId, @RequestBody Place place,
                                                      @RequestHeader("X-User-Id") String requesterId,
                                                      @RequestHeader("X-User-Role") String role) {
        Place existingPlace = placeService.getPlaceByPlaceId(placeId);
        if (!AccessControlUtil.isAdminOrSelf(role, requesterId, existingPlace.getCreatedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Place updatedPlace = placeService.updatePlaceByPlaceId(placeId, place);
        return ResponseEntity.ok(updatedPlace);
    }

    @PutMapping(value="/name/{placeName}")
    public ResponseEntity<Place> updatePlaceByPlaceName(@PathVariable String placeName, @RequestBody Place place,
                                                        @RequestHeader("X-User-Id") String requesterId,
                                                        @RequestHeader("X-User-Role") String role) {
        Place existingPlace = placeService.getPlaceByPlaceId(placeName);
        if (!AccessControlUtil.isAdminOrSelf(role, requesterId, existingPlace.getCreatedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Place updatedPlace = placeService.updatePlaceByPlaceName(placeName, place);
        return ResponseEntity.ok(updatedPlace);
    }


    @DeleteMapping("/id/{placeID}")
    public ResponseEntity<String> deletePlaceByPlaceId(@PathVariable String placeId,
                                                       @RequestHeader("X-User-Id") String requesterId,
                                                       @RequestHeader("X-User-Role") String role) {
        Place place = placeService.getPlaceByPlaceId(placeId);

        if (!AccessControlUtil.isAdminOrSelf(role, requesterId, place.getCreatedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only creator or admin can delete this place.");
        }
        placeService.deletePlaceByPlaceId(placeId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Place deleted successfully");
    }

    @DeleteMapping("/name/{placeName}")
    public ResponseEntity<String> deletePlaceByPlaceName(@PathVariable String placeName,
                                                         @RequestHeader("X-User-Id") String requesterId,
                                                         @RequestHeader("X-User-Role") String role) {
        Place place = placeService.getPlaceByPlaceName(placeName);
        if (!AccessControlUtil.isAdminOrSelf(role, requesterId, place.getCreatedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only creator or admin can delete this place.");
        }
        placeService.deletePlaceByPlaceName(placeName);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Place deleted successfully");
    }

}
