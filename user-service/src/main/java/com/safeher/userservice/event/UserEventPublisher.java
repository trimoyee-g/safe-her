package com.safeher.userservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${app.kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${app.kafka.topics.user-deleted}")
    private String userDeletedTopic;

    public void publishUserCreated(UserCreatedEvent event) {
        send(userCreatedTopic, event.getUserId(), event);
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        send(userUpdatedTopic, event.getUserId(), event);
    }

    public void publishUserDeleted(UserDeletedEvent event) {
        send(userDeletedTopic, event.getUserId(), event);
    }

    private void send(String topic, UUID key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key.toString(), payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic [{}] key [{}]: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Published event to topic [{}] partition [{}] offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
