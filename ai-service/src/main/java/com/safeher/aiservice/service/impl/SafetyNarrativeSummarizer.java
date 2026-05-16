package com.safeher.aiservice.service.impl;

import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.PlaceServiceClient;
import com.safeher.aiservice.client.RatingServiceClient;
import com.safeher.aiservice.event.PlaceSummaryUpdatedEvent;
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
 * Agent 2: Safety Narrative Summarizer
 *
 * Triggered on every Nth new review (configurable).
 * Reads up to 50 most recent reviews, asks Claude to synthesise a
 * 2–3 sentence plain-language safety summary.
 *
 * Result is broadcast over Kafka so place-service can update without a synchronous call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyNarrativeSummarizer {

    private final OllamaClient       ollamaClient;
    private final RatingServiceClient   ratingServiceClient;
    private final PlaceServiceClient    placeServiceClient;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate   redis;

    @Value("${app.kafka.topics.place-summary-updated}")          private String summaryTopic;
    @Value("${app.agents.summarizer.min-reviews-to-summarize:5}") private int minReviews;
    @Value("${app.agents.summarizer.regenerate-every-n-reviews:10}") private int regenerateEvery;
    @Value("${app.ollama.models.summarization}") private String model;

    private static final String COUNTER_KEY = "ai:summary:counter:";
    private static final String SYSTEM_PROMPT = """
            You are a safety analyst for safeher, a platform helping people —
            especially women — assess the safety of public places.
            
            Given a collection of user reviews about a specific place, write a concise
            2–3 sentence safety summary that:
            1. States the overall safety impression neutrally and factually
            2. Highlights the most commonly mentioned positive safety factors
            3. Notes any significant safety concerns if present
            
            Be specific (mention exact concerns like "poor lighting on the eastern side"
            or "security guards present at both entrances") rather than vague.
            
            Do NOT include any preamble, greeting or sign-off. Output the summary text only.
            Maximum 80 words.
            """;

    @Async
    public void maybeRegenerateSummary(UUID placeId, int currentTotalRatings) {
        if (currentTotalRatings < minReviews) {
            log.debug("Place [{}] has only {} reviews – skipping summary", placeId, currentTotalRatings);
            return;
        }

        // Increment review counter in Redis; only regenerate every N reviews
        String counterKey = COUNTER_KEY + placeId;
        Long count = redis.opsForValue().increment(counterKey);
        redis.expire(counterKey, 30, TimeUnit.DAYS);

        if (count == null || (count % regenerateEvery != 0 && count != 1)) {
            log.debug("Place [{}] counter={} – not regenerating summary yet", placeId, count);
            return;
        }

        generateAndStore(placeId);
    }

    public String generateAndStore(UUID placeId) {
        try {
            var ratingsPage = ratingServiceClient.getRatingsByPlace(placeId, "NEWEST", 0, 50);
            List<RatingServiceClient.RatingDto> reviews = ratingsPage.content();

            if (reviews.size() < minReviews) return null;

            PlaceServiceClient.PlaceDto place = placeServiceClient.getPlace(placeId);
            String reviewsText = formatReviews(reviews);

            String userMessage = """
                    Place: %s (%s) in %s, %s
                    Overall safety score: %.1f/5.0 based on %d reviews
                    
                    Reviews:
                    %s
                    """.formatted(
                    place.name(), place.category(),
                    place.city() != null ? place.city() : "unknown city",
                    place.country() != null ? place.country() : "unknown country",
                    place.safetyScore(), reviews.size(),
                    reviewsText);

            String summary = ollamaClient.complete(SYSTEM_PROMPT, userMessage, 200, model);
            summary = summary.trim();

            // Broadcast via Kafka
            kafka.send(summaryTopic, placeId.toString(), PlaceSummaryUpdatedEvent.builder()
                    .placeId(placeId)
                    .summary(summary)
                    .reviewsAnalysed(reviews.size())
                    .build());

            log.info("Generated AI summary for place [{}] ({} reviews analysed)", placeId, reviews.size());
            return summary;

        } catch (Exception ex) {
            log.error("Summary generation failed for place [{}]: {}", placeId, ex.getMessage());
            return null;
        }
    }

    private String formatReviews(List<RatingServiceClient.RatingDto> reviews) {
        return reviews.stream()
                .filter(r -> r.body() != null && !r.body().isBlank())
                .limit(30) // cap tokens
                .map(r -> {
                    String tags = r.tags() != null && !r.tags().isEmpty()
                            ? " [tags: " + String.join(", ", r.tags()) + "]"
                            : "";
                    return String.format("Score %d/5%s: %s", r.score(), tags,
                            r.body().length() > 300 ? r.body().substring(0, 300) + "…" : r.body());
                })
                .collect(Collectors.joining("\n---\n"));
    }
}
