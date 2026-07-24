package su.ternovskii.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import su.ternovskii.notificationservice.dto.kafka.NotificationEvent;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    @Qualifier("objectKafkaTemplate")
    private final KafkaTemplate<String, Object> eventKafkaTemplate;

    public void publish(String eventType, Long notificationId, String channel,
                        String recipient, int attemptNumber,
                        Long executionTimeMs, String errorMessage) {
        NotificationEvent event = new NotificationEvent(
                eventType, notificationId, channel, recipient,
                attemptNumber, executionTimeMs, errorMessage, Instant.now()
        );
        // fire-and-forget: не ждём подтверждения, не блокируем бизнес-логику
        eventKafkaTemplate.send("notification.events",
                String.valueOf(notificationId), event);
        log.debug("Published event {} for notificationId={}", eventType, notificationId);
    }
}