package com.safeher.aiservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ReviewAssistRequest {
    @NotNull private UUID placeId;
    @Min(1) @Max(5) private int score;
    private List<String> selectedTags;
    @Size(max = 500) private String partialBody;
}
