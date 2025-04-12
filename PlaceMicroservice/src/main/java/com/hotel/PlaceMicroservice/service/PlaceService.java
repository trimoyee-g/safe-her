package com.hotel.PlaceMicroservice.service;

import com.hotel.PlaceMicroservice.entity.Place;

import java.util.List;

public interface PlaceService {
    Place addPlace(Place place);
    Place updatePlace(String id, Place place);
    void deletePlace(String id);
    List<Place> getAll();
    Place getPlace(String id);
}
