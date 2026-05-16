package com.safeher.aiservice.service.impl;

import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.PlaceServiceClient;
import com.safeher.aiservice.event.PlaceDescriptionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Agent 6: Place Description Generator
 *
 * When a new place is created (via Kafka place.created event),
 * generates a concise factual description if none was provided.
 * Also callable on-demand from the admin panel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceDescriptionGenerator {

    private final OllamaClient    ollamaClient;
    private final PlaceServiceClient placeServiceClient;
    private final KafkaTemplate<String, Object> kafka;

    @Value("${app.ollama.models.description}")              private String model;
    @Value("${app.kafka.topics.place-description-updated}") private String descriptionTopic;

    private static final String SYSTEM_PROMPT = """
            You are a local knowledge assistant for safeher, a safety-rating platform.
            
            Given a place's name, category, and location, write a factual, neutral
            2-sentence description that helps users understand what kind of place it is
            and its general context.
            
            Focus on facts relevant to safety assessment (e.g. "busy commercial area",
            "residential neighborhood", "major transit hub", "industrial zone").
            Do NOT make safety judgements — that's for the reviews.
            Output the description text only. No preamble.
            Maximum 40 words.
            """;

    @Async
    public void generateForNewPlace(UUID placeId) {
        try {
            PlaceServiceClient.PlaceDto place = placeServiceClient.getPlace(placeId);

            // Only generate if no description exists
            // (Place Service would need to expose this field; simplified here)
            String userMessage = String.format(
                    "Place name: %s\nCategory: %s\nCity: %s\nCountry: %s",
                    place.name(), place.category(),
                    place.city()    != null ? place.city()    : "unknown",
                    place.country() != null ? place.country() : "unknown");

            String description = ollamaClient.complete(SYSTEM_PROMPT, userMessage, 80, model).trim();

            kafka.send(descriptionTopic, placeId.toString(), PlaceDescriptionUpdatedEvent.builder()
                    .placeId(placeId)
                    .description(description)
                    .build());

            log.info("Generated description for place [{}]", placeId);

        } catch (Exception ex) {
            log.warn("Description generation failed for place [{}]: {}", placeId, ex.getMessage());
        }
    }

    public String generateOnDemand(UUID placeId) {
        PlaceServiceClient.PlaceDto place = placeServiceClient.getPlace(placeId);
        String userMessage = String.format(
                "Place name: %s\nCategory: %s\nCity: %s\nCountry: %s",
                place.name(), place.category(),
                place.city()    != null ? place.city()    : "unknown",
                place.country() != null ? place.country() : "unknown");
        String description = ollamaClient.complete(SYSTEM_PROMPT, userMessage, 80, model).trim();

        kafka.send(descriptionTopic, placeId.toString(), PlaceDescriptionUpdatedEvent.builder()
                .placeId(placeId)
                .description(description)
                .build());

        log.info("Generated description on-demand for place [{}]", placeId);
        return description;
    }
}
