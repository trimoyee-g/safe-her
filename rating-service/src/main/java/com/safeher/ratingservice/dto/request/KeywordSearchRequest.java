package com.safeher.ratingservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class KeywordSearchRequest {
    @NotBlank(message = "Query is required")
    @Size(min = 2, max = 200)
    private String query;

    private UUID placeId;

    @Min(1) @Max(5)
    private Integer minScore;

    @Min(1) @Max(5)
    private Integer maxScore;

    @Min(0) private int page = 0;
    @Min(1) @Max(50) private int size = 20;
}
