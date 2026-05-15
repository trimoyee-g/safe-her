package com.safeher.ratingservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateRatingRequest {

    @NotNull(message = "placeId is required")
    private UUID placeId;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must be at most 5")
    private Integer score;

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Review body must not exceed 2000 characters")
    private String body;

    @Size(max = 5, message = "Maximum 5 photos allowed")
    private List<String> photos;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<String> tags;

    private boolean anonymous = false;
}
