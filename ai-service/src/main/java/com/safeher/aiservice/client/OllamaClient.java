package com.safeher.aiservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama client – 100% free, runs locally via Docker.
 *
 * All 6 agents call the same complete() / completeWithHistory() methods.
 *
 * Recommended models (pick based on your RAM):
 *   llama3.2:3b   – 2 GB  – fast, good for classification tasks (agents 1,4)
 *   llama3.1:8b   – 5 GB  – better reasoning, good for summarisation (agents 2,5)
 *   mistral:7b    – 4 GB  – strong instruction-following, good all-rounder
 *   gemma2:9b     – 6 GB  – excellent for multilingual (Hindi, Bengali support)
 *   phi3:mini     – 2 GB  – very fast, lighter reasoning
 *
 * Ollama API is OpenAI-compatible at /api/chat.
 * Uses the "messages" format with system + user roles.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${app.ollama.base-url:http://localhost:11434}") private String baseUrl;
    @Value("${app.ollama.model:llama3.2:3b}")              private String model;
    @Value("${app.ollama.timeout-seconds:60}")             private int    timeoutSeconds;

    private static final String CHAT_PATH = "/api/chat";

    // ── Primary API ──────────────────────────────────────────────────────────

    /**
     * Single-turn completion with a system prompt.
     * Used by: ReviewModerationAgent, SafetyNarrativeSummarizer,
     *          SmartReviewAssistant, RatingAnomalyDetector, PlaceDescriptionGenerator
     */
    @CircuitBreaker(name = "ollama", fallbackMethod = "completeFallback")
    @Retry(name = "ollama")
    public String complete(String systemPrompt, String userMessage) {
        return complete(systemPrompt, userMessage, 1024);
    }

    @CircuitBreaker(name = "ollama", fallbackMethod = "completeFallbackWithTokens")
    @Retry(name = "ollama")
    public String complete(String systemPrompt, String userMessage, int maxTokens) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system",  "content", systemPrompt),
                Map.of("role", "user",    "content", userMessage)
        );
        return chat(messages, maxTokens);
    }

    /**
     * Multi-turn completion with full conversation history.
     * Used by: SafetyChatbotService
     */
    @CircuitBreaker(name = "ollama", fallbackMethod = "streamFallback")
    @Retry(name = "ollama")
    public String completeWithHistory(String systemPrompt,
                                      List<Map<String, String>> history,
                                      int maxTokens) {
        // Prepend system message
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        return chat(messages, maxTokens);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private String chat(List<Map<String, String>> messages, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model",    model,
                "messages", messages,
                "stream",   false,          // we want the full response at once
                "options",  Map.of(
                        "num_predict", maxTokens,
                        "temperature", 0.1,   // low temp → deterministic, good for classification
                        "top_p",       0.9
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("Ollama request → model={} messages={}", model, messages.size());

        ResponseEntity<OllamaResponse> response = restTemplate.exchange(
                baseUrl + CHAT_PATH,
                HttpMethod.POST,
                request,
                OllamaResponse.class
        );

        if (response.getBody() == null || response.getBody().getMessage() == null) {
            throw new IllegalStateException("Empty response from Ollama");
        }

        String text = response.getBody().getMessage().getContent();
        log.debug("Ollama response ({} chars)", text.length());
        return text;
    }

    // ── Fallbacks (circuit open / all retries exhausted) ─────────────────────

    public String completeFallback(String system, String user, Throwable ex) {
        log.error("Ollama circuit open or unavailable: {}", ex.getMessage());
        return "[AI analysis temporarily unavailable – Ollama may be starting up]";
    }

    public String completeFallbackWithTokens(String system, String user, int tokens, Throwable ex) {
        log.error("Ollama circuit open or unavailable: {}", ex.getMessage());
        return "[AI analysis temporarily unavailable – Ollama may be starting up]";
    }

    public String streamFallback(String system, List<Map<String, String>> messages,
                                  int tokens, Throwable ex) {
        log.error("Ollama circuit open: {}", ex.getMessage());
        return "I'm temporarily unavailable. Please try again in a moment.";
    }

    // ── Response DTO ─────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OllamaResponse {
        private OllamaMessage message;

        @JsonProperty("done")
        private boolean done;

        @JsonProperty("total_duration")
        private long totalDuration;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OllamaMessage {
            private String role;
            private String content;
        }
    }
}
