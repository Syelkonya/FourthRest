package su.ternovskii.notificationservice.dto.kafka;

public record NotificationCommand(
        Long notificationId,
        String channel,
        String recipient,
        String message
) {}