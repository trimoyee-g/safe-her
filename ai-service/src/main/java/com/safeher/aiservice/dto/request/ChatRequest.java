package com.safeher.aiservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

// ── Chat ──────────────────────────────────────────────────────────────────────

@Data
public class ChatRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message too long")
    private String message;

    private List<UUID> placeIds;       // optional – places being discussed

    private List<ChatMessage> history; // previous turns (frontend maintains this)
}
