package com.hotel.PlaceMicroservice.repository;

import com.hotel.PlaceMicroservice.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {

    Optional<Place> findByPlaceName(String placeName);
    boolean existsByPlaceName(String placeName);
    void deleteByPlaceName(String placeName);

}
