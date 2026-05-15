package com.safeher.placeservice.repository.elasticsearch;

import com.safeher.placeservice.entity.PlaceDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceSearchRepository extends ElasticsearchRepository<PlaceDocument, String> {

    Page<PlaceDocument> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Page<PlaceDocument> findByActiveTrueAndCategory(String category, Pageable pageable);

    Page<PlaceDocument> findByActiveTrueAndCity(String city, Pageable pageable);
}
