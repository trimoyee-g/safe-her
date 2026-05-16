package com.safeher.ratingservice.event.listener;

import com.safeher.ratingservice.event.ReviewFlaggedEvent;
import com.safeher.ratingservice.exception.RatingNotFoundException;
import com.safeher.ratingservice.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes AI moderation decisions from ReviewModerationAgent and
 * RatingAnomalyDetector (both publish to ai.review.flagged).
 *
 * autoSuppressed=true  → suppress the rating (active=false, removed from search index)
 * autoSuppressed=false → flag the rating for human review queue
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiEventListener {

    private final RatingService ratingService;

    @KafkaListener(
        topics           = "${app.kafka.topics.ai-review-flagged}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReviewFlagged(
            @Payload  ReviewFlaggedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        log.debug("Received ai.review.flagged: ratingId={} reason={} confidence={} autoSuppressed={}",
                event.getRatingId(), event.getReason(), event.getConfidence(), event.isAutoSuppressed());

        try {
            if (event.isAutoSuppressed()) {
                ratingService.suppress(event.getRatingId());
            } else {
                ratingService.flag(event.getRatingId(), event.getReason(), event.getConfidence());
            }
        } catch (RatingNotFoundException ex) {
            log.warn("ai.review.flagged for unknown rating [{}] – skipping", event.getRatingId());
        }
    }
}
