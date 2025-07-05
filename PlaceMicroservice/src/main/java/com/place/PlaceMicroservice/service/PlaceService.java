package com.hotel.PlaceMicroservice.service;

import com.hotel.PlaceMicroservice.entity.Place;

import java.util.List;

public interface PlaceService {
    Place addPlace(Place place);
    Place updatePlaceByPlaceId(String id, Place place);
    Place updatePlaceByPlaceName(String placeName, Place place);
    void deletePlaceByPlaceId(String id);
    void deletePlaceByPlaceName(String name);
    List<Place> getAll();
    Place getPlaceByPlaceId(String id);
    Place getPlaceByPlaceName(String placeName);
}
