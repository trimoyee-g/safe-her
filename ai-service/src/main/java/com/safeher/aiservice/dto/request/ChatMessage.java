package com.safeher.aiservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessage {
    @NotBlank private String role;     // "user" | "assistant"
    @NotBlank private String content;
}
