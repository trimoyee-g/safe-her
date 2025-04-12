package com.hotel.PlaceMicroservice.service.impl;

import com.hotel.PlaceMicroservice.entity.Place;
import com.hotel.PlaceMicroservice.exceptions.ResourceNotFoundException;
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

    @Override
    public Place addPlace(Place place) {
        String randomPlaceID = UUID.randomUUID().toString();
        place.setId(randomPlaceID);
        return placeRepository.save(place);
    }

    @Override
    public Place updatePlace(String id, Place place){
        return placeRepository.findById(id)
                .map(existingPlace -> {
                    existingPlace.setName(place.getName());
                    existingPlace.setLocation(place.getLocation());
                    existingPlace.setAbout(place.getAbout());
                    return placeRepository.save(existingPlace);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Place with given ID is not found on server"));
    }

    @Override
    public void deletePlace(String id) {
        if (!placeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Place with given ID is not found on server");
        }
        placeRepository.deleteById(id);
    }

    @Override
    public List<Place> getAll() {
        return placeRepository.findAll();
    }

    @Override
    public Place getPlace(String id) {
        return placeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Place with given ID does not exist on server"));
    }
}
