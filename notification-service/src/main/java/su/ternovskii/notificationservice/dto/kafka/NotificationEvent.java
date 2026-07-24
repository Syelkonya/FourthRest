package su.ternovskii.notificationservice.dto.kafka;

import java.time.Instant;

public record NotificationEvent(
    String eventType,        // "NOTIFICATION_CREATED", "COMMAND_SENT",
                             // "DELIVERY_SUCCESS", "DELIVERY_FAILED", "RETRY_SCHEDULED"
    Long notificationId,
    String channel,          // "SMS", "PUSH", "EMAIL" — может быть null для общих событий
    String recipient,
    int attemptNumber,
    Long executionTimeMs,    // время выполнения (может быть null)
    String errorMessage,     // причина ошибки (если есть, иначе null)
    Instant timestamp
) {}