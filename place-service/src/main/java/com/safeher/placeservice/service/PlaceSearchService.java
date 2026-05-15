package com.safeher.placeservice.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.safeher.placeservice.dto.request.KeywordSearchRequest;
import com.safeher.placeservice.dto.response.PagedResponse;
import com.safeher.placeservice.dto.response.PlaceSummaryResponse;
import com.safeher.placeservice.entity.PlaceDocument;
import com.safeher.placeservice.mapper.PlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceSearchService {

    private final ElasticsearchOperations esOps;
    private final PlaceMapper placeMapper;

    /**
     * Multi-field keyword search across name, description, address, city.
     * Supports optional category and city filters.
     */
    public PagedResponse<PlaceSummaryResponse> search(KeywordSearchRequest req) {

        // ── Build bool query ─────────────────────────────────────────────────
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Must: active = true
        bool.filter(TermQuery.of(t -> t.field("active").value(true))._toQuery());

        // Must: multi-match on text fields
        bool.must(MultiMatchQuery.of(m -> m
            .query(req.getQuery())
            .fields("name^3", "name.search^2", "description", "address", "city")
            .type(TextQueryType.BestFields)
            .fuzziness("AUTO")
        )._toQuery());

        // Optional: category filter
        if (req.getCategory() != null) {
            bool.filter(TermQuery.of(t -> t
                .field("category")
                .value(req.getCategory().name()))._toQuery());
        }

        // Optional: city filter
        if (req.getCity() != null && !req.getCity().isBlank()) {
            bool.filter(TermQuery.of(t -> t
                .field("city")
                .value(req.getCity()))._toQuery());
        }

        // Optional: country filter
        if (req.getCountry() != null && !req.getCountry().isBlank()) {
            bool.filter(TermQuery.of(t -> t
                .field("country")
                .value(req.getCountry()))._toQuery());
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(req.getPage(), req.getSize()))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("safetyScore").order(SortOrder.Desc)))
                .build();

        SearchHits<PlaceDocument> hits = esOps.search(nativeQuery, PlaceDocument.class);

        List<PlaceSummaryResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::docToSummary)
                .toList();

        long total = hits.getTotalHits();
        int  totalPages = (int) Math.ceil((double) total / req.getSize());

        return PagedResponse.<PlaceSummaryResponse>builder()
                .content(content)
                .page(req.getPage())
                .size(req.getSize())
                .totalElements(total)
                .totalPages(totalPages)
                .last(req.getPage() >= totalPages - 1)
                .build();
    }

    // ── Doc → summary (ES doc doesn't have full geo point in the summary format) ──

    private PlaceSummaryResponse docToSummary(PlaceDocument doc) {
        return PlaceSummaryResponse.builder()
                .id(java.util.UUID.fromString(doc.getId()))
                .name(doc.getName())
                .address(doc.getAddress())
                .city(doc.getCity())
                .country(doc.getCountry())
                .latitude(doc.getLocation() != null ? doc.getLocation().getLat() : 0)
                .longitude(doc.getLocation() != null ? doc.getLocation().getLon() : 0)
                .safetyScore(doc.getSafetyScore())
                .totalRatings(doc.getTotalRatings())
                .verified(doc.isVerified())
                .build();
    }
}
