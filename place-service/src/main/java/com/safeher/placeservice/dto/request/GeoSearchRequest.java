package com.safeher.placeservice.dto.request;

import com.safeher.placeservice.enums.PlaceCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GeoSearchRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")  @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    /** Radius in metres. Defaults to 5 km; capped at 50 km server-side. */
    @Positive(message = "Radius must be positive")
    private Double radiusMeters = 5000.0;

    private PlaceCategory category;

    @Min(0) private int page = 0;
    @Min(1) @Max(100) private int size = 20;
}
