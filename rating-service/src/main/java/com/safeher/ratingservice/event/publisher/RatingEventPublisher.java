package com.safeher.ratingservice.event.publisher;

import com.safeher.ratingservice.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.rating-created}") private String createdTopic;
    @Value("${app.kafka.topics.rating-updated}") private String updatedTopic;
    @Value("${app.kafka.topics.rating-deleted}") private String deletedTopic;

    public void publishCreated(RatingCreatedEvent event) {
        send(createdTopic, event.getPlaceId(), event);
    }

    public void publishUpdated(RatingUpdatedEvent event) {
        send(updatedTopic, event.getPlaceId(), event);
    }

    public void publishDeleted(RatingDeletedEvent event) {
        send(deletedTopic, event.getPlaceId(), event);
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload)
                .whenComplete((res, ex) -> {
                    if (ex != null) log.error("Kafka publish failed [{}]: {}", topic, ex.getMessage());
                    else log.debug("Published → [{}] key [{}]", topic, key);
                });
    }
}
