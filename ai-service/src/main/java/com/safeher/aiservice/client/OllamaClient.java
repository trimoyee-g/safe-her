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
 * Supports per-agent model routing:
 *   Agent                 Model (configured in application.yml)
 *   ─────────────────     ──────────────────────────────────────
 *   Moderation            phi3:mini     – fast classification, low RAM
 *   Anomaly detection     phi3:mini     – same: structured JSON output
 *   Review assistant      phi3:mini     – short prompt, needs speed
 *   Place description     phi3:mini     – simple generation task
 *   Summarisation         llama3.1:8b  – needs reading comprehension
 *   Chatbot               mistral:7b   – multi-turn, instruction following
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${app.ollama.base-url:http://localhost:11434}") private String baseUrl;
    @Value("${app.ollama.model:llama3.2:3b}")               private String defaultModel;

    private static final String CHAT_PATH = "/api/chat";

    // ── Primary API ───────────────────────────────────────────────────────────

    /**
     * Single-turn, default model, default token limit.
     * Convenience overload – agents that don't need model routing use this.
     */
    @CircuitBreaker(name = "ollama", fallbackMethod = "completeFallback")
    @Retry(name = "ollama")
    public String complete(String systemPrompt, String userMessage) {
        return chat(buildMessages(systemPrompt, userMessage), 1024, defaultModel);
    }

    /**
     * Single-turn with explicit model and token limit.
     * Used by all agents that specify their own model via application.yml.
     */
    @CircuitBreaker(name = "ollama", fallbackMethod = "completeFallbackFull")
    @Retry(name = "ollama")
    public String complete(String systemPrompt, String userMessage,
                           int maxTokens, String model) {
        return chat(buildMessages(systemPrompt, userMessage), maxTokens, model);
    }

    /**
     * Multi-turn with explicit model and token limit.
     * Used by SafetyChatbotService.
     */
    @CircuitBreaker(name = "ollama", fallbackMethod = "completeWithHistoryFallback")
    @Retry(name = "ollama")
    public String completeWithHistory(String systemPrompt,
                                      List<Map<String, String>> history,
                                      int maxTokens,
                                      String model) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        return chat(messages, maxTokens, model);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String chat(List<Map<String, String>> messages, int maxTokens, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model",    model,
                "messages", messages,
                "stream",   false,   // RestTemplate reads a single response body – never stream
                "options",  Map.of(
                        "num_predict", maxTokens,
                        "temperature", 0.1,  // low temp → deterministic, good for classification
                        "top_p",       0.9
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        log.debug("Ollama → model={} messages={} maxTokens={}", model, messages.size(), maxTokens);

        ResponseEntity<OllamaResponse> response = restTemplate.exchange(
                baseUrl + CHAT_PATH,
                HttpMethod.POST,
                request,
                OllamaResponse.class
        );

        if (response.getBody() == null || response.getBody().getMessage() == null) {
            throw new IllegalStateException("Empty response from Ollama (model=" + model + ")");
        }

        String text = response.getBody().getMessage().getContent();
        log.debug("Ollama ← {} chars (model={})", text.length(), model);
        return text;
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userMessage) {
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userMessage)
        );
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────
    // Signature must match the annotated method exactly (same params + Throwable appended)

    public String completeFallback(String system, String user, Throwable ex) {
        log.error("Ollama unavailable (default model): {}", ex.getMessage());
        return "[AI analysis temporarily unavailable]";
    }

    public String completeFallbackFull(String system, String user,
                                       int maxTokens, String model, Throwable ex) {
        log.error("Ollama unavailable (model={}): {}", model, ex.getMessage());
        return "[AI analysis temporarily unavailable]";
    }

    public String completeWithHistoryFallback(String system,
                                              List<Map<String, String>> history,
                                              int maxTokens, String model,
                                              Throwable ex) {
        log.error("Ollama unavailable for chatbot (model={}): {}", model, ex.getMessage());
        return "I'm temporarily unavailable. Please try again in a moment.";
    }

    // ── Response DTO ──────────────────────────────────────────────────────────

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