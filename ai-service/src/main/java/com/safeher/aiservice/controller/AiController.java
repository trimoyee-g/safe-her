package com.safeher.aiservice.controller;

import com.safeher.aiservice.dto.request.ChatRequest;
import com.safeher.aiservice.dto.request.ModerationRequest;
import com.safeher.aiservice.dto.request.ReviewAssistRequest;
import com.safeher.aiservice.dto.response.ApiResponse;
import com.safeher.aiservice.dto.response.ChatResponse;
import com.safeher.aiservice.service.impl.*;
import com.safeher.aiservice.dto.request.*;
import com.safeher.aiservice.dto.response.*;
import com.safeher.aiservice.service.impl.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final SafetyChatbotService chatbotService;
    private final SmartReviewAssistant reviewAssistant;
    private final SafetyNarrativeSummarizer summarizer;
    private final ReviewModerationAgent moderationAgent;
    private final PlaceDescriptionGenerator descriptionGenerator;

    // ── Agent 5: Safety Chatbot ───────────────────────────────────────────────

    /**
     * POST /api/v1/ai/chat
     * Conversational safety assistant with multi-turn history.
     * Requires auth. Rate limited per user.
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "ai-api")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication auth) {
        UUID callerId = (UUID) auth.getCredentials();
        ChatResponse response = chatbotService.chat(request, callerId);
        return ApiResponse.ok(response);
    }

    // ── Agent 3: Smart Review Assistant ──────────────────────────────────────

    /**
     * POST /api/v1/ai/review-assist
     * Returns a writing prompt based on partial review state.
     * Requires auth. Short response (< 1s target).
     */
    @PostMapping("/review-assist")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "ai-api")
    public ApiResponse<String> reviewAssist(
            @Valid @RequestBody ReviewAssistRequest request) {
        String prompt = reviewAssistant.getWritingPrompt(
                request.getPlaceId(),
                request.getScore(),
                request.getSelectedTags(),
                request.getPartialBody());
        return ApiResponse.ok(prompt);
    }

    // ── Agent 2: Safety Narrative Summary (on-demand) ─────────────────────────

    /**
     * POST /api/v1/ai/places/{placeId}/summary
     * Triggers immediate summary regeneration for a place.
     * Public – no auth required (cached result; triggers background job).
     */
    @PostMapping("/places/{placeId}/summary")
    @RateLimiter(name = "ai-api")
    public ApiResponse<String> generateSummary(@PathVariable UUID placeId) {
        String summary = summarizer.generateAndStore(placeId);
        if (summary == null) {
            return ApiResponse.error("Not enough reviews to generate a summary yet");
        }
        return ApiResponse.ok("Summary generated", summary);
    }

    // ── Agent 1: Manual moderation trigger (admin) ────────────────────────────

    /**
     * POST /api/v1/ai/moderate
     * Admin endpoint to manually trigger moderation of a specific review.
     */
    @PostMapping("/moderate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> moderate(@Valid @RequestBody ModerationRequest request) {
        moderationAgent.moderate(
                request.getRatingId(),
                request.getPlaceId(),
                request.getUserId(),
                request.getScore(),
                request.getTitle(),
                request.getBody(),
                request.getTags());
        return ApiResponse.ok("Moderation queued", null);
    }

    // ── Agent 6: Place description (admin) ────────────────────────────────────

    /**
     * POST /api/v1/ai/places/{placeId}/description
     * Generate or regenerate a place description. Admin only.
     */
    @PostMapping("/places/{placeId}/description")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> generateDescription(@PathVariable UUID placeId) {
        String desc = descriptionGenerator.generateOnDemand(placeId);
        return ApiResponse.ok("Description generated", desc);
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("AI Service is running");
    }
}
