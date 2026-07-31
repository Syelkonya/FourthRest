package su.ternovskii.eventservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import su.ternovskii.eventservice.dto.NotificationEvent;
import su.ternovskii.eventservice.entity.NotificationEventEntity;
import su.ternovskii.eventservice.repository.NotificationEventRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventListener {

    private final NotificationEventRepository repository;

    @KafkaListener(topics = "notification.events", groupId = "event-service")
    public void handle(NotificationEvent event) {
        log.info("Received event: type={}, notificationId={}, channel={}",
                event.eventType(), event.notificationId(), event.channel());

        NotificationEventEntity entity = new NotificationEventEntity();
        entity.setEventType(event.eventType());
        entity.setNotificationId(event.notificationId());
        entity.setChannel(event.channel());
        entity.setRecipient(event.recipient());
        entity.setAttemptNumber(event.attemptNumber());
        entity.setExecutionTimeMs(event.executionTimeMs());
        entity.setErrorMessage(event.errorMessage());
        entity.setEventTimestamp(event.timestamp());

        repository.save(entity);
    }
}
