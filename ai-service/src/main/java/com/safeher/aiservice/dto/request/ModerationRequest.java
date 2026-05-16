package com.safeher.aiservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ModerationRequest {
    @NotBlank private String ratingId;
    @NotNull  private UUID   placeId;
    @NotNull  private UUID   userId;
    @Min(1) @Max(5) private int score;
    private String       title;
    @Size(max = 2000) private String body;
    private List<String> tags;
}
