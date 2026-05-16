package com.safeher.aiservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.RatingServiceClient;
import com.safeher.aiservice.event.ReviewFlaggedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Agent 4: Rating Anomaly Detector
 *
 * Two-stage detection:
 *
 * Stage 1 (Redis – instant): counts ratings per place in a rolling window.
 *   If velocity > threshold, promote to Stage 2.
 *
 * Stage 2 (Claude – async): fetches the suspicious batch and asks Claude
 *   whether the pattern looks coordinated (similar language, all same score,
 *   all new accounts, implausible score/sentiment mix).
 *   If confirmed → flag all reviews in the batch.
 *
 * This protects a safety platform where fake high scores could endanger lives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingAnomalyDetector {

    private final OllamaClient       ollamaClient;
    private final RatingServiceClient   ratingServiceClient;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate   redis;
    private final ObjectMapper          objectMapper;

    @Value("${app.kafka.topics.review-flagged}")                      private String flaggedTopic;
    @Value("${app.agents.anomaly-detector.velocity-window-minutes:60}") private int windowMinutes;
    @Value("${app.agents.anomaly-detector.velocity-threshold:15}")      private int velocityThreshold;

    private static final String VELOCITY_KEY = "ai:velocity:";

    private static final String SYSTEM_PROMPT = """
            You are a fraud detection AI for safeher, a women's safety platform.
            
            You will receive a batch of recent reviews for a single place that was flagged
            for unusually high submission velocity. Determine whether this batch shows signs
            of coordinated manipulation:
            
            Signs of coordination:
            - Multiple reviews with near-identical language or phrasing
            - All reviews giving the same extreme score (all 5 or all 1)
            - Score clearly contradicts the review text sentiment
            - Reviews lack any specific local knowledge
            - Implausibly positive reviews for a place with a prior low score history
            
            Respond ONLY with a JSON object:
            {
              "coordinated": true|false,
              "confidence": 0.0-1.0,
              "suspicious_ids": ["ratingId1", "ratingId2"],
              "reason": "brief explanation"
            }
            
            Be conservative — only flag when clearly evident. False positives harm genuine users.
            """;

    @Async
    public void checkVelocity(String ratingId, UUID placeId) {
        String key = VELOCITY_KEY + placeId;

        // Increment velocity counter with expiry = rolling window
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, windowMinutes, TimeUnit.MINUTES);
        }

        log.debug("Velocity for place [{}] = {}/{} in {}min window",
                placeId, count, velocityThreshold, windowMinutes);

        if (count != null && count >= velocityThreshold && count % velocityThreshold == 0) {
            log.warn("High velocity for place [{}]: {} ratings in {}min – triggering anomaly check",
                    placeId, count, windowMinutes);
            analyzeAnomaly(placeId);
        }
    }

    private void analyzeAnomaly(UUID placeId) {
        try {
            var page = ratingServiceClient.getRatingsByPlace(placeId, "NEWEST", 0, 30);
            List<RatingServiceClient.RatingDto> recentReviews = page.content();

            if (recentReviews.isEmpty()) return;

            String batchText = recentReviews.stream()
                    .map(r -> String.format(
                            "[id:%s] score=%d tags=%s body=\"%s\"",
                            r.id(), r.score(),
                            r.tags() != null ? r.tags() : List.of(),
                            r.body() != null ? r.body().substring(0, Math.min(200, r.body().length())) : ""))
                    .collect(Collectors.joining("\n"));

            String raw = ollamaClient.complete(SYSTEM_PROMPT,
                    "Recent reviews batch for place " + placeId + ":\n" + batchText, 400);

            JsonNode result = objectMapper.readTree(extractJson(raw));

            boolean coordinated = result.path("coordinated").asBoolean(false);
            double  confidence  = result.path("confidence").asDouble(0);
            String  reason      = result.path("reason").asText("");

            if (!coordinated || confidence < 0.70) {
                log.debug("Anomaly check for place [{}]: NOT coordinated (confidence={:.2f})", placeId, confidence);
                return;
            }

            log.warn("COORDINATED RATINGS detected for place [{}] confidence={:.2f} reason={}",
                    placeId, confidence, reason);

            // Flag each suspicious review
            result.path("suspicious_ids").forEach(idNode -> {
                String sid = idNode.asText();
                kafka.send(flaggedTopic, sid, ReviewFlaggedEvent.builder()
                        .ratingId(sid)
                        .placeId(placeId)
                        .reason("COORDINATED")
                        .confidence(confidence)
                        .autoSuppressed(false) // human review required for batch actions
                        .build());
            });

        } catch (Exception ex) {
            log.error("Anomaly detection failed for place [{}]: {}", placeId, ex.getMessage());
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        return (start >= 0 && end > start) ? raw.substring(start, end + 1) : raw;
    }
}
