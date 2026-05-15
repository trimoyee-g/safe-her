package com.safeher.ratingservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Aggregated safety score summary – returned alongside individual reviews */
@Data
@Builder
public class PlaceRatingSummary {
    private UUID placeId;
    private BigDecimal averageScore;
    private int totalRatings;
    private Map<Integer, Integer> scoreDistribution; // {1:3, 2:5, 3:10, 4:20, 5:40}
}
