package su.ternovskii.eventservice.dto;

import java.time.Instant;

public record NotificationEvent(
        String eventType,
        Long notificationId,
        String channel,
        String recipient,
        int attemptNumber,
        Long executionTimeMs,
        String errorMessage,
        Instant timestamp
) {}
