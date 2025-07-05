package com.place.PlaceMicroservice.service;

import com.place.PlaceMicroservice.entity.Place;

import java.util.List;

public interface PlaceService {

    // create
    Place addPlace(Place place);

    // read
    List<Place> getAll();
    Place getPlaceByPlaceId(String id);
    Place getPlaceByPlaceName(String placeName);

    // update
    Place updatePlaceByPlaceId(String id, Place place);
    Place updatePlaceByPlaceName(String placeName, Place place);

    // delete
    void deletePlaceByPlaceId(String id);
    void deletePlaceByPlaceName(String name);

}
