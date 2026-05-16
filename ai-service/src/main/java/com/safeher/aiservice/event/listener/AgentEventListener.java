package com.safeher.aiservice.event.listener;

import com.safeher.aiservice.event.PlaceCreatedEvent;
import com.safeher.aiservice.event.RatingCreatedEvent;
import com.safeher.aiservice.service.impl.PlaceDescriptionGenerator;
import com.safeher.aiservice.service.impl.RatingAnomalyDetector;
import com.safeher.aiservice.service.impl.ReviewModerationAgent;
import com.safeher.aiservice.service.impl.SafetyNarrativeSummarizer;
import com.safeher.aiservice.service.impl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Central Kafka listener for the AI Service.
 *
 * rating.created → ReviewModerationAgent    (async, non-blocking)
 *               → SafetyNarrativeSummarizer (async, non-blocking)
 *               → RatingAnomalyDetector     (async, non-blocking)
 *
 * place.created  → PlaceDescriptionGenerator (async, non-blocking)
 *
 * All agent calls are @Async so Kafka offset commits are not delayed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentEventListener {

    private final ReviewModerationAgent moderationAgent;
    private final SafetyNarrativeSummarizer summarizer;
    private final RatingAnomalyDetector anomalyDetector;
    private final PlaceDescriptionGenerator descriptionGenerator;

    @KafkaListener(
        topics           = "${app.kafka.topics.rating-created}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRatingCreated(
            @Payload  RatingCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        log.debug("AI processing rating.created: ratingId={} placeId={} score={}",
                event.getRatingId(), event.getPlaceId(), event.getScore());

        // Each call is @Async – returns immediately, runs in thread pool
        moderationAgent.moderate(
                event.getRatingId(),
                event.getPlaceId(),
                event.getUserId(),
                event.getScore(),
                null,   // title not in event – would be fetched inside agent if needed
                null,   // body  not in event – same
                null);  // tags

        summarizer.maybeRegenerateSummary(event.getPlaceId(), event.getTotalRatings());

        anomalyDetector.checkVelocity(event.getRatingId(), event.getPlaceId());
    }

    @KafkaListener(
        topics           = "${app.kafka.topics.place-created}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPlaceCreated(
            @Payload  PlaceCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.debug("AI processing place.created: placeId={} name={}", event.getPlaceId(), event.getName());
        descriptionGenerator.generateForNewPlace(event.getPlaceId());
    }
}
