package com.safeher.placeservice.dto.request;

import com.safeher.placeservice.enums.PlaceCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class KeywordSearchRequest {

    @NotBlank(message = "Search query is required")
    @Size(min = 2, max = 200)
    private String query;

    private PlaceCategory category;
    private String city;
    private String country;

    @Min(0) private int page = 0;
    @Min(1) @Max(100) private int size = 20;
}
