package com.safeher.ratingservice.repository.elasticsearch;

import com.safeher.ratingservice.entity.document.RatingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingSearchRepository extends ElasticsearchRepository<RatingDocument, String> {

    Page<RatingDocument> findByPlaceIdAndActiveTrue(String placeId, Pageable pageable);

    Page<RatingDocument> findByActiveTrueAndScoreGreaterThanEqual(int minScore, Pageable pageable);
}
