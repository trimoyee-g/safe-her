package com.safeher.placeservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.safeher.placeservice.enums.PlaceCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceSummaryResponse {
    private UUID id;
    private String name;
    private PlaceCategory category;
    private String address;
    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private BigDecimal safetyScore;
    private int totalRatings;
    private boolean verified;
    private String avatarPhoto;
    /** Populated on geo-search */
    private Double distanceMeters;
}
