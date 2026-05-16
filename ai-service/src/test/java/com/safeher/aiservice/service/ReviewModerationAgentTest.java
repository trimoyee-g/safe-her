package com.safeher.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeher.aiservice.client.OllamaClient;
import com.safeher.aiservice.client.RatingServiceClient;
import com.safeher.aiservice.event.ReviewFlaggedEvent;
import com.safeher.aiservice.service.impl.ReviewModerationAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewModerationAgentTest {

    @Mock OllamaClient                       ollamaClient;
    @Mock RatingServiceClient                   ratingServiceClient;
    @Mock KafkaTemplate<String, Object>         kafka;

    @InjectMocks ReviewModerationAgent agent;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(agent, "flaggedTopic", "ai.review.flagged");
        ReflectionTestUtils.setField(agent, "autoSuppressThreshold", 0.90);
        ReflectionTestUtils.setField(agent, "flagThreshold", 0.65);
        ReflectionTestUtils.setField(agent, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("CLEAN review – no Kafka event published")
    void cleanReview_noAction() {
        when(ollamaClient.complete(any(), any(), anyInt()))
                .thenReturn("{\"classification\":\"CLEAN\",\"confidence\":0.95,\"reason\":\"Genuine review\"}");

        agent.moderate("r1", UUID.randomUUID(), UUID.randomUUID(), 4,
                "Safe station", "Well lit, guards present.", List.of("well-lit"));

        verify(kafka, never()).send(anyString(), anyString(), any());
        verify(ratingServiceClient, never()).suppressRating(any());
    }

    @Test
    @DisplayName("SPAM review above flag threshold – publishes flagged event")
    void spamReview_aboveThreshold_publishesEvent() {
        when(ollamaClient.complete(any(), any(), anyInt()))
                .thenReturn("{\"classification\":\"SPAM\",\"confidence\":0.78,\"reason\":\"Off-topic promotional\"}");

        UUID placeId = UUID.randomUUID();
        UUID userId  = UUID.randomUUID();
        agent.moderate("r2", placeId, userId, 5, null, "Buy now at discount!", null);

        verify(kafka).send(eq("ai.review.flagged"), eq("r2"), any(ReviewFlaggedEvent.class));
        verify(ratingServiceClient, never()).suppressRating(any()); // below auto-suppress threshold
    }

    @Test
    @DisplayName("ABUSE review above auto-suppress threshold – suppresses and publishes")
    void abuseReview_aboveAutoSuppress_suppressesAndPublishes() {
        when(ollamaClient.complete(any(), any(), anyInt()))
                .thenReturn("{\"classification\":\"ABUSE\",\"confidence\":0.95,\"reason\":\"Hate speech\"}");

        UUID placeId = UUID.randomUUID();
        agent.moderate("r3", placeId, UUID.randomUUID(), 1, null, "Hate content here", null);

        verify(ratingServiceClient).suppressRating("r3");

        ArgumentCaptor<ReviewFlaggedEvent> captor = ArgumentCaptor.forClass(ReviewFlaggedEvent.class);
        verify(kafka).send(eq("ai.review.flagged"), eq("r3"), captor.capture());

        ReviewFlaggedEvent event = captor.getValue();
        assertThat(event.isAutoSuppressed()).isTrue();
        assertThat(event.getReason()).isEqualTo("ABUSE");
        assertThat(event.getConfidence()).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    @DisplayName("Review with no text – skipped silently")
    void noTextReview_skipped() {
        agent.moderate("r4", UUID.randomUUID(), UUID.randomUUID(), 3, null, null, null);

        verify(ollamaClient, never()).complete(any(), any(), anyInt());
        verify(kafka, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Low confidence – below flag threshold – no action")
    void lowConfidence_belowThreshold_noAction() {
        when(ollamaClient.complete(any(), any(), anyInt()))
                .thenReturn("{\"classification\":\"SPAM\",\"confidence\":0.40,\"reason\":\"Possibly promotional\"}");

        agent.moderate("r5", UUID.randomUUID(), UUID.randomUUID(), 5,
                null, "Some ambiguous content", null);

        verify(kafka, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("ollama API failure – handled gracefully, no exception thrown")
    void apiFailure_handledGracefully() {
        when(ollamaClient.complete(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Upstream timeout"));

        // Should not throw
        agent.moderate("r6", UUID.randomUUID(), UUID.randomUUID(), 2,
                null, "Some review text", null);
    }
}
