package su.ternovskii.pushadapter.dto;

public record NotificationCommand(Long notificationId, String channel, String recipient, String message) {}
