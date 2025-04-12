package com.hotel.PlaceMicroservice.repository;

import com.hotel.PlaceMicroservice.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {
}
