package com.safeher.ratingservice.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.safeher.ratingservice.dto.request.KeywordSearchRequest;
import com.safeher.ratingservice.entity.document.RatingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingSearchService {

    private final ElasticsearchOperations esOps;

    public SearchHits<RatingDocument> search(KeywordSearchRequest req) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Must: active = true
        bool.filter(TermQuery.of(t -> t.field("active").value(true))._toQuery());

        // Must: multi-match over title and body
        bool.must(MultiMatchQuery.of(m -> m
                .query(req.getQuery())
                .fields("title^2", "body", "tags")
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO")
        )._toQuery());

        // Optional: filter by placeId
        if (req.getPlaceId() != null) {
            bool.filter(TermQuery.of(t -> t
                    .field("placeId")
                    .value(req.getPlaceId().toString()))._toQuery());
        }

        // Optional: score range filter
        if (req.getMinScore() != null || req.getMaxScore() != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field("score");
            if (req.getMinScore() != null) range.gte(co.elastic.clients.json.JsonData.of(req.getMinScore()));
            if (req.getMaxScore() != null) range.lte(co.elastic.clients.json.JsonData.of(req.getMaxScore()));
            bool.filter(range.build()._toQuery());
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(req.getPage(), req.getSize()))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)))
                .build();

        return esOps.search(query, RatingDocument.class);
    }
}
