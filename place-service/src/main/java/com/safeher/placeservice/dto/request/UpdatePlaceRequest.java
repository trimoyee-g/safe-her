package com.safeher.placeservice.dto.request;

import com.safeher.placeservice.enums.PlaceCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdatePlaceRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private PlaceCategory category;
    private String subCategory;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    @DecimalMin(value = "-90.0")  @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    private String phoneNumber;

    @Size(max = 500)
    private String website;

    private Map<String, String> openingHours;

    @Size(max = 10)
    private List<String> photos;

    @Size(max = 20)
    private List<String> amenities;
}
