package su.ternovskii.notificationservice.dto.kafka;

public record NotificationResult(
    Long notificationId,
    String channel,       // "SMS", "PUSH", "EMAIL" — строка, не enum
    boolean success,
    String errorMessage   // null если success=true
) {}