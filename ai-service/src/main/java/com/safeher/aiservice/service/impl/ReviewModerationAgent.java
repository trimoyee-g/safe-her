package com.safeher.aiservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.RatingServiceClient;
import com.safeher.aiservice.event.ReviewFlaggedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Agent 1: Review Moderation
 *
 * Triggered by rating.created Kafka events.
 * Calls Claude to classify the review text as:
 *   - CLEAN        – publish normally
 *   - SPAM         – low-effort, off-topic
 *   - ABUSE        – harassment, hate speech
 *   - FAKE         – score/sentiment mismatch, implausible
 *   - COORDINATED  – pattern suggests brigading (used by anomaly detector too)
 *
 * Above auto-suppress-threshold → suppress immediately and publish ai.review.flagged
 * Above flag-threshold          → flag for admin review queue
 * Below both                    → no action
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewModerationAgent {

    private final OllamaClient      ollamaClient;
    private final RatingServiceClient  ratingServiceClient;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper         objectMapper;

    @Value("${app.kafka.topics.review-flagged}")           private String flaggedTopic;
    @Value("${app.agents.moderation.auto-suppress-threshold:0.90}") private double autoSuppressThreshold;
    @Value("${app.agents.moderation.flag-threshold:0.65}")          private double flagThreshold;
    @Value("${app.ollama.models.moderation}") private String model;

    private static final String SYSTEM_PROMPT = """
            You are a content moderation AI for safeher, a safety-rating platform where women
            and marginalised communities share experiences about public places.
            
            Your task is to classify a user-submitted review as one of:
            - CLEAN: genuine, relevant, appropriate review
            - SPAM: low effort, irrelevant, or promotional content
            - ABUSE: harassment, hate speech, threats, or discriminatory language
            - FAKE: score clearly does not match the review sentiment; implausible claims
            - COORDINATED: appears to be part of a brigading attempt
            
            Context matters: discussing safety concerns including street harassment, poor lighting,
            feeling unsafe, or intimidation is ALWAYS legitimate and must be classified as CLEAN.
            
            Respond ONLY with a JSON object, no other text:
            {
              "classification": "CLEAN|SPAM|ABUSE|FAKE|COORDINATED",
              "confidence": 0.0-1.0,
              "reason": "one sentence explanation"
            }
            """;

    @Async
    public void moderate(String ratingId, UUID placeId, UUID userId,
                         int score, String title, String body, java.util.List<String> tags) {

        if ((body == null || body.isBlank()) && (title == null || title.isBlank())) {
            log.debug("Review [{}] has no text content – skipping moderation", ratingId);
            return;
        }

        String reviewText = buildReviewText(score, title, body, tags);

        try {
            String raw = ollamaClient.complete(SYSTEM_PROMPT, reviewText, 256, model);
            JsonNode result = objectMapper.readTree(extractJson(raw));

            String classification = result.path("classification").asText("CLEAN");
            double confidence     = result.path("confidence").asDouble(0.0);
            String reason         = result.path("reason").asText("");

            log.info("Moderation [ratingId={}] → {} (confidence={:.2f})", ratingId, classification, confidence);

            if ("CLEAN".equals(classification)) return;

            boolean autoSuppressed = confidence >= autoSuppressThreshold;

            if (confidence >= flagThreshold) {
                if (autoSuppressed) {
                    ratingServiceClient.suppressRating(ratingId);
                    log.warn("Auto-suppressed review [{}] reason={}", ratingId, reason);
                }

                kafka.send(flaggedTopic, ratingId, ReviewFlaggedEvent.builder()
                        .ratingId(ratingId)
                        .placeId(placeId)
                        .userId(userId)
                        .reason(classification)
                        .confidence(confidence)
                        .autoSuppressed(autoSuppressed)
                        .build());
            }

        } catch (Exception ex) {
            log.error("Moderation failed for rating [{}]: {}", ratingId, ex.getMessage());
        }
    }

    private String buildReviewText(int score, String title, String body, java.util.List<String> tags) {
        StringBuilder sb = new StringBuilder();
        sb.append("Safety score given: ").append(score).append("/5\n");
        if (title != null && !title.isBlank()) sb.append("Title: ").append(title).append("\n");
        if (body  != null && !body.isBlank())  sb.append("Review: ").append(body).append("\n");
        if (tags  != null && !tags.isEmpty())  sb.append("Tags: ").append(String.join(", ", tags));
        return sb.toString();
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        return raw;
    }
}
