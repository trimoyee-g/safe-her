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

    @PutMapping("/{placeId}")
    public ResponseEntity<Place> updatePlace(@PathVariable String placeId, @RequestBody Place place) {
        Place updatedPlace = placeService.updatePlace(placeId, place);
        return ResponseEntity.ok(updatedPlace);
    }

    @GetMapping(value = "/{placeID}")
    public ResponseEntity<Place> getPlace(@PathVariable String placeID){
        Place place = placeService.getPlace(placeID);
        return ResponseEntity.ok(place);
    }


    @GetMapping
    public ResponseEntity<List<Place>> getAll() {
        List<Place> allPlaces = placeService.getAll();
        return ResponseEntity.ok(allPlaces);
    }

    @DeleteMapping("/{placeID}")
    public ResponseEntity<String> deleteUser(@PathVariable String placeID) {
        placeService.deletePlace(placeID);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Place deleted successfully");
    }

}
