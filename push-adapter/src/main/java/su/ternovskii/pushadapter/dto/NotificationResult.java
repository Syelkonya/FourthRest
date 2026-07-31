package su.ternovskii.pushadapter.dto;

public record NotificationResult(
    Long notificationId,
    String channel,
    boolean success,
    String errorMessage
) {}