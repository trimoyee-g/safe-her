package com.safeher.aiservice.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private String message;
    private List<UUID> suggestedPlaceIds;
}
