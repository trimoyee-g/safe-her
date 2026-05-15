package com.safeher.placeservice.dto.request;

import com.safeher.placeservice.enums.PlaceCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreatePlaceRequest {

    @NotBlank(message = "Place name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private PlaceCategory category;

    private String subCategory;

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;

    private String phoneNumber;

    @Size(max = 500)
    private String website;

    private Map<String, String> openingHours;

    @Size(max = 10, message = "Maximum 10 photos allowed")
    private List<String> photos;

    @Size(max = 20, message = "Maximum 20 amenities allowed")
    private List<String> amenities;
}
