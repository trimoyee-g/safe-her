package com.safeher.placeservice.event.publisher;

import com.safeher.placeservice.event.PlaceCreatedEvent;
import com.safeher.placeservice.event.PlaceDeletedEvent;
import com.safeher.placeservice.event.PlaceScoreUpdatedEvent;
import com.safeher.placeservice.event.PlaceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.safeher.placeservice.event.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.place-created}")      private String createdTopic;
    @Value("${app.kafka.topics.place-updated}")      private String updatedTopic;
    @Value("${app.kafka.topics.place-deleted}")      private String deletedTopic;
    @Value("${app.kafka.topics.place-score-updated}")private String scoreTopic;

    public void publishPlaceCreated(PlaceCreatedEvent event) {
        send(createdTopic, event.getPlaceId(), event);
    }

    public void publishPlaceUpdated(PlaceUpdatedEvent event) {
        send(updatedTopic, event.getPlaceId(), event);
    }

    public void publishPlaceDeleted(PlaceDeletedEvent event) {
        send(deletedTopic, event.getPlaceId(), event);
    }

    public void publishScoreUpdated(PlaceScoreUpdatedEvent event) {
        send(scoreTopic, event.getPlaceId(), event);
    }

    private void send(String topic, UUID key, Object payload) {
        CompletableFuture<?> future = kafkaTemplate.send(topic, key.toString(), payload);
        future.whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to [{}] key [{}]: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Published event to [{}] key [{}]", topic, key);
            }
        });
    }
}
