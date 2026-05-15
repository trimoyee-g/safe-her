package com.safeher.placeservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.safeher.placeservice.enums.PlaceCategory;
import com.safeher.placeservice.enums.PlaceSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceResponse {

    private UUID id;
    private String externalId;
    private PlaceSource source;
    private String name;
    private String description;
    private PlaceCategory category;
    private String subCategory;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private String website;
    private String googleMapsUrl;
    private Map<String, String> openingHours;
    private List<String> photos;
    private List<String> amenities;
    private BigDecimal safetyScore;
    private int totalRatings;
    private boolean active;
    private boolean verified;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** Distance in metres from the search origin – populated on geo-search only */
    private Double distanceMeters;
}
