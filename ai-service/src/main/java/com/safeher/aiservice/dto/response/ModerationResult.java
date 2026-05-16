package com.safeher.aiservice.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationResult {
    private String  classification;
    private double  confidence;
    private String  reason;
    private boolean autoSuppressed;
}
