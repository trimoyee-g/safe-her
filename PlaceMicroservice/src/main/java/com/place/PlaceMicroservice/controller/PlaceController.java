package com.hotel.PlaceMicroservice.controller;

import com.hotel.PlaceMicroservice.entity.Place;
import com.hotel.PlaceMicroservice.service.PlaceService;
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
    public ResponseEntity<Place> addPlace(@RequestBody Place place){
        return ResponseEntity.status(HttpStatus.CREATED).body(placeService.addPlace(place));
    }

    @PutMapping(value="/id/{placeId}")
    public ResponseEntity<Place> updatePlaceByPlaceId(@PathVariable String placeId, @RequestBody Place place) {
        Place updatedPlace = placeService.updatePlaceByPlaceId(placeId, place);
        return ResponseEntity.ok(updatedPlace);
    }

    @PutMapping(value="/name/{placeName}")
    public ResponseEntity<Place> updatePlaceByPlaceName(@PathVariable String placeName, @RequestBody Place place) {
        Place updatedPlace = placeService.updatePlaceByPlaceName(placeName, place);
        return ResponseEntity.ok(updatedPlace);
    }

    @GetMapping(value = "/id/{placeId}")
    public ResponseEntity<Place> getPlaceByPlaceId(@PathVariable String placeId){
        Place place = placeService.getPlaceByPlaceId(placeId);
        return ResponseEntity.ok(place);
    }

    @GetMapping(value = "/name/{placeName}")
    public ResponseEntity<Place> getPlaceByPlaceName(@PathVariable String placeName){
        Place place = placeService.getPlaceByPlaceName(placeName);
        return ResponseEntity.ok(place);
    }


    @GetMapping
    public ResponseEntity<List<Place>> getAll() {
        List<Place> allPlaces = placeService.getAll();
        return ResponseEntity.ok(allPlaces);
    }

    @DeleteMapping("/id/{placeID}")
    public ResponseEntity<String> deletePlaceByPlaceId(@PathVariable String placeID) {
        placeService.deletePlaceByPlaceId(placeID);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Place deleted successfully");
    }

    @DeleteMapping("/name/{placeName}")
    public ResponseEntity<String> deletePlaceByPlaceName(@PathVariable String placeName) {
        placeService.deletePlaceByPlaceName(placeName);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Place deleted successfully");
    }

}
