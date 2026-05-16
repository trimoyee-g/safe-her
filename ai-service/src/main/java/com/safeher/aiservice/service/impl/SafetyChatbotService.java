package com.safeher.aiservice.service.impl;

import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.PlaceServiceClient;
import com.safeher.aiservice.client.RatingServiceClient;
import com.safeher.aiservice.dto.request.ChatMessage;
import com.safeher.aiservice.dto.request.ChatRequest;
import com.safeher.aiservice.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 5: Safety Chatbot
 *
 * Conversational RAG agent. The frontend maintains chat history and sends it
 * on each turn. This service:
 *
 * 1. Extracts any place names or location references from the user's question
 * 2. Fetches relevant place + review data from downstream services
 * 3. Builds a context-enriched prompt
 * 4. Returns Claude's response
 *
 * Example queries:
 * - "Is it safe to walk from Park Street Metro to Sudder Street at 11pm?"
 * - "What do people say about safety at Sealdah Station?"
 * - "Which parks in Kolkata have the best safety ratings?"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyChatbotService {

    private final OllamaClient ollamaClient;
    private final RatingServiceClient  ratingServiceClient;
    private final PlaceServiceClient   placeServiceClient;

    private static final int MAX_HISTORY_TURNS = 10;
    private static final int CHATBOT_MAX_TOKENS = 512;

    @Value("${app.ollama.models.chatbot}") private String model;

    private static final String SYSTEM_PROMPT = """
            You are SafeGuide, a friendly and knowledgeable safety assistant on safeher –
            a platform helping people, especially women, make informed decisions about
            the safety of public places.
            
            You have access to real crowdsourced safety reviews and ratings from our platform.
            When answering questions:
            
            1. Ground your answers in the review data provided. Quote specific concerns
               reviewers mentioned (e.g. "Several reviewers noted poor lighting near the
               northern exit after 9pm").
            2. Be honest about uncertainty. If there isn't enough data, say so.
            3. Give practical, actionable advice (which exit to use, what time is safer,
               what to be aware of).
            4. Never downplay genuine safety concerns to seem positive.
            5. If you don't have data about a place, say you don't have reviews for it yet
               and encourage the user to add one.
            6. Keep responses concise – 2–4 short paragraphs maximum.
            7. You do not provide emergency advice. For emergencies, always direct users
               to call 100 (India police) or 112 (emergency).
            
            Context about the places relevant to this conversation:
            {{PLACE_CONTEXT}}
            """;

    public ChatResponse chat(ChatRequest request, UUID callerUserId) {
        // Build context from any placeIds referenced in the request
        String placeContext = buildPlaceContext(request.getPlaceIds());

        String systemWithContext = SYSTEM_PROMPT.replace("{{PLACE_CONTEXT}}", placeContext);

        // Convert history to ollama message format (cap at MAX_HISTORY_TURNS)
        List<Map<String, String>> messages = buildMessageHistory(request.getHistory());
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        String response = ollamaClient.completeWithHistory(
                systemWithContext, messages, CHATBOT_MAX_TOKENS, model);

        log.debug("Chatbot response for user [{}]: {} chars", callerUserId, response.length());

        return ChatResponse.builder()
                .message(response.trim())
                .suggestedPlaceIds(request.getPlaceIds()) // echo back for frontend linking
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildPlaceContext(List<UUID> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return "No specific places referenced yet. Answer based on general safety principles.";
        }

        StringBuilder ctx = new StringBuilder();
        for (UUID placeId : placeIds) {
            try {
                PlaceServiceClient.PlaceDto place = placeServiceClient.getPlace(placeId);
                var ratingsPage = ratingServiceClient.getRatingsByPlace(placeId, "NEWEST", 0, 20);

                ctx.append(String.format("\n## %s (%s)\n", place.name(), place.category()));
                ctx.append(String.format("Location: %s, %s\n", place.city(), place.country()));
                ctx.append(String.format("Safety score: %.1f/5 based on %d reviews\n\n",
                        place.safetyScore(), place.totalRatings()));

                String reviewSnippets = ratingsPage.content().stream()
                        .filter(r -> r.body() != null && !r.body().isBlank())
                        .limit(10)
                        .map(r -> {
                            String tags = r.tags() != null && !r.tags().isEmpty()
                                    ? "[" + String.join(", ", r.tags()) + "] "
                                    : "";
                            String snippet = r.body().length() > 250
                                    ? r.body().substring(0, 250) + "…"
                                    : r.body();
                            return String.format("- Score %d/5 %s: %s", r.score(), tags, snippet);
                        })
                        .collect(Collectors.joining("\n"));

                if (!reviewSnippets.isBlank()) {
                    ctx.append("Recent reviews:\n").append(reviewSnippets).append("\n");
                } else {
                    ctx.append("No detailed review text available for this place yet.\n");
                }

            } catch (Exception ex) {
                log.warn("Could not fetch context for place [{}]: {}", placeId, ex.getMessage());
                ctx.append("\n## Place ").append(placeId).append(": data unavailable\n");
            }
        }

        return ctx.toString();
    }

    private List<Map<String, String>> buildMessageHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return new ArrayList<>();

        return history.stream()
                .skip(Math.max(0, history.size() - MAX_HISTORY_TURNS * 2)) // keep last N turns
                .map(msg -> Map.of(
                        "role",    msg.getRole().toLowerCase(),
                        "content", msg.getContent()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
