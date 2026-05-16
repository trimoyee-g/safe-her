package com.safeher.aiservice.service.impl;

import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.PlaceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Agent 3: Smart Review Assistant
 *
 * Called from the frontend as the user fills in their review.
 * Given the partial review (score + selected tags), returns a
 * short, friendly prompt to help the user write a more useful review.
 *
 * This is a synchronous, low-latency call (< 1s target).
 * Uses a compact prompt and 100-token limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartReviewAssistant {

    private final OllamaClient    ollamaClient;
    private final PlaceServiceClient placeServiceClient;

    @Value("${app.ollama.models.assistant}") private String model;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant on safeher, a women's safety platform.
            Your job is to give users a short, warm, specific writing prompt (1 sentence,
            max 20 words) that helps them write a more useful safety review.
            
            Base the prompt on the score and tags they have selected so far.
            Never be preachy. Focus on practical safety details other users would find helpful.
            Examples of good prompts:
            - "Can you describe what the lighting is like near the entrance?"
            - "Which exit felt safer, and why?"
            - "Was security or police presence noticeable?"
            
            Output the prompt sentence only. No preamble.
            """;

    public String getWritingPrompt(UUID placeId, int score, List<String> selectedTags,
                                   String partialBody) {
        PlaceServiceClient.PlaceDto place = placeServiceClient.getPlace(placeId);

        String userMessage = """
                Place: %s (%s)
                Score selected: %d/5
                Tags selected: %s
                Partial review text: %s
                """.formatted(
                place.name(),
                place.category(),
                score,
                selectedTags != null ? String.join(", ", selectedTags) : "none",
                partialBody != null && !partialBody.isBlank() ? partialBody : "(none yet)");

        try {
            return ollamaClient.complete(SYSTEM_PROMPT, userMessage, 60, model).trim();
        } catch (Exception ex) {
            log.warn("SmartReviewAssistant failed for place [{}]: {}", placeId, ex.getMessage());
            return "What specific details would help someone feel confident about this place?";
        }
    }
}
