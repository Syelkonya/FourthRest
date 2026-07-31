package su.ternovskii.emailadapter.dto;

public record NotificationResult(
    Long notificationId,
    String channel,
    boolean success,
    String errorMessage
) {}