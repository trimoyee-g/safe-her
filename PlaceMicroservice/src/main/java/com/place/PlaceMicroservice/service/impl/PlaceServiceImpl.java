package com.hotel.PlaceMicroservice.service.impl;

import com.hotel.PlaceMicroservice.entity.Place;
import com.hotel.PlaceMicroservice.entity.Rating;
import com.hotel.PlaceMicroservice.exceptions.ResourceNotFoundException;
import com.hotel.PlaceMicroservice.externalservices.RatingService;
import com.hotel.PlaceMicroservice.repository.PlaceRepository;
import com.hotel.PlaceMicroservice.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PlaceServiceImpl implements PlaceService {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private RatingService ratingService;



    @Override
    public Place addPlace(Place place) {
        String randomPlaceID = UUID.randomUUID().toString();
        place.setPlaceId(randomPlaceID);
        return placeRepository.save(place);
    }

    @Override
    public Place updatePlaceByPlaceName(String placeName, Place place) {
        return placeRepository.findByPlaceName(placeName)
                .map(existingPlace -> {
                    existingPlace.setPlaceName(place.getPlaceName());
                    existingPlace.setPlaceLocation(place.getPlaceLocation());
                    existingPlace.setPlaceAbout(place.getPlaceAbout());
                    return placeRepository.save(existingPlace);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Place with given name is not found on server"));
    }

    @Override
    public void deletePlaceByPlaceName(String name) {
        if (!placeRepository.existsByPlaceName(name)) {
            throw new ResourceNotFoundException("Place with given name is not found on server");
        }
        placeRepository.deleteByPlaceName(name);
    }

    @Override
    public Place updatePlaceByPlaceId(String id, Place place){
        return placeRepository.findById(id)
                .map(existingPlace -> {
                    existingPlace.setPlaceName(place.getPlaceName());
                    existingPlace.setPlaceLocation(place.getPlaceLocation());
                    existingPlace.setPlaceAbout(place.getPlaceAbout());
                    return placeRepository.save(existingPlace);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Place with given ID is not found on server"));
    }

    @Override
    public void deletePlaceByPlaceId(String id) {
        if (!placeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Place with given ID is not found on server");
        }
        placeRepository.deleteById(id);
    }

    @Override
    public List<Place> getAll() {
        List<Place> places = placeRepository.findAll();

        for (Place place : places) {
            List<Rating> ratings = ratingService.getRatingsByPlaceId(place.getPlaceId());
            place.setRatings(ratings);  // Make sure Place entity has a `ratings` field
        }

        return places;
    }

    @Override
    public Place getPlaceByPlaceName(String placeName) {
        Place place = placeRepository.findByPlaceName(placeName)
                .orElseThrow(() -> new ResourceNotFoundException("Place with given name does not exist on server"));

        List<Rating> ratings = ratingService.getRatingsByPlaceId(place.getPlaceId());
        place.setRatings(ratings);

        return place;
    }


    @Override
    public Place getPlaceByPlaceId(String id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place with given ID does not exist on server"));

        List<Rating> ratings = ratingService.getRatingsByPlaceId(place.getPlaceId());
        place.setRatings(ratings);

        return place;
    }

}
