package com.safeher.placeservice.event.listener;

import com.safeher.placeservice.event.RatingCreatedEvent;
import com.safeher.placeservice.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingEventListener {

    private final PlaceService placeService;

    /**
     * Consumes rating.created events from the Rating Service.
     * Updates the materialised safety_score and total_ratings on the Place row
     * and re-indexes the Elasticsearch document — all without a synchronous
     * Feign call at read time.
     */
    @KafkaListener(
        topics           = "${app.kafka.topics.rating-created}",
        groupId          = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRatingCreated(@Payload RatingCreatedEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received rating.created for placeId={} newAvg={} total={}",
                event.getPlaceId(), event.getNewAverageScore(), event.getTotalRatings());

        try {
            placeService.updateSafetyScore(
                event.getPlaceId(),
                BigDecimal.valueOf(event.getNewAverageScore()),
                event.getTotalRatings()
            );
        } catch (Exception ex) {
            log.error("Failed to update safety score for place [{}]: {}", event.getPlaceId(), ex.getMessage(), ex);
            // Let the container retry per the consumer config; in prod add a DLT handler
            throw ex;
        }
    }
}
