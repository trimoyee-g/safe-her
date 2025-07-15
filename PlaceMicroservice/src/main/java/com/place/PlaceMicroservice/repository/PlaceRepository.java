package com.place.PlaceMicroservice.repository;

import com.place.PlaceMicroservice.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {

    Optional<Place> findByPlaceName(String placeName);
    boolean existsByPlaceName(String placeName);
    void deleteByPlaceName(String placeName);

    @Query(value = "SELECT * FROM micro_places p WHERE LOWER(p.user_name) LIKE LOWER(CONCAT('%', :name, '%')) LIMIT 5", nativeQuery = true)
    List<Place> getPlacesByName(@Param("name") String name);

}
