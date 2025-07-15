package com.user.UserMicroservice.externalservices;

import com.user.UserMicroservice.entity.Place;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "PLACEMICROSERVICE")
public interface PlaceService {

    // create
    @PostMapping("/places")
    public Place addPlace(Place place);


    // read
    @GetMapping("/places")
    public List<Place> getAllPlaces();

    @GetMapping("/places/id/{placeId}")
    public Place getPlaceByPlaceId(@PathVariable String placeId);

    @GetMapping("/places/name/{placeName}")
    public Place getPlaceByPlaceName(@PathVariable String placeName);

    @GetMapping("/places/search")
    public ResponseEntity<?> getPlacesByName(@RequestParam("name") String name);


    // update
    @PutMapping("/places/id/{placeId}")
    public Place updatePlaceByPlaceId(@PathVariable String placeId, Place place);

    @PutMapping("/places/name/{placeName}")
    public Place updatePlaceByPlaceName(@PathVariable String placeName, Place place);


    // delete
    @DeleteMapping("places/id/{placeId}")
    public void deletePlace(@PathVariable String placeId);

    @DeleteMapping("places/name/{placeName}")
    public void deletePlaceByPlaceName(@PathVariable String placeName);
}
