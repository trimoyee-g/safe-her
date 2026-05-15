package com.safeher.authservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.auth-registered}")       private String registeredTopic;
    @Value("${app.kafka.topics.auth-deleted}")          private String deletedTopic;
    @Value("${app.kafka.topics.auth-password-changed}") private String passwordChangedTopic;

    public void publishUserRegistered(AuthUserRegisteredEvent event) {
        send(registeredTopic, event.getAuthUserId(), event);
    }

    public void publishUserDeleted(AuthUserDeletedEvent event) {
        send(deletedTopic, event.getAuthUserId(), event);
    }

    public void publishPasswordChanged(AuthPasswordChangedEvent event) {
        send(passwordChangedTopic, event.getAuthUserId(), event);
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload)
                .whenComplete((res, ex) -> {
                    if (ex != null) log.error("Kafka publish failed [{}] key [{}]: {}", topic, key, ex.getMessage());
                    else log.debug("Published event → [{}] key [{}]", topic, key);
                });
    }
}
