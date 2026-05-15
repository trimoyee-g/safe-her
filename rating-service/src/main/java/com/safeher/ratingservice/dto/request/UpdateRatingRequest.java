package com.safeher.ratingservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class UpdateRatingRequest {
    @Min(1) @Max(5)
    private Integer score;

    @Size(max = 150)
    private String title;

    @Size(max = 2000)
    private String body;

    @Size(max = 5)
    private List<String> photos;

    @Size(max = 10)
    private List<String> tags;

    private Boolean anonymous;
}
