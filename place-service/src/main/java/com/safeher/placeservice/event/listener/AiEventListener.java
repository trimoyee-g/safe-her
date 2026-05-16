package com.safeher.placeservice.event.listener;

import com.safeher.placeservice.event.PlaceDescriptionUpdatedEvent;
import com.safeher.placeservice.event.PlaceSummaryUpdatedEvent;
import com.safeher.placeservice.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiEventListener {

    private final PlaceService placeService;

    /**
     * Consumes ai.place.summary.updated events from the AI Service.
     * Persists the generated summary to Postgres and re-indexes in Elasticsearch.
     */
    @KafkaListener(
        topics           = "${app.kafka.topics.ai-summary-updated}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAiSummaryUpdated(@Payload PlaceSummaryUpdatedEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received ai.place.summary.updated for placeId={} ({} reviews analysed)",
                event.getPlaceId(), event.getReviewsAnalysed());

        try {
            placeService.updateAiSummary(
                    event.getPlaceId(),
                    event.getSummary(),
                    event.getOccurredAt() != null ? event.getOccurredAt() : OffsetDateTime.now()
            );
        } catch (Exception ex) {
            log.error("Failed to apply AI summary for place [{}]: {}", event.getPlaceId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Consumes ai.place.description.updated events from the AI Service.
     * Persists the generated description to Postgres and re-indexes in Elasticsearch.
     */
    @KafkaListener(
        topics           = "${app.kafka.topics.place-description-updated}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDescriptionUpdated(@Payload PlaceDescriptionUpdatedEvent event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received ai.place.description.updated for placeId={}", event.getPlaceId());

        try {
            placeService.updateDescription(event.getPlaceId(), event.getDescription());
        } catch (Exception ex) {
            log.error("Failed to apply description for place [{}]: {}", event.getPlaceId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
