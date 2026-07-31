package su.ternovskii.smsadapter.dto;

public record NotificationCommand(Long notificationId, String channel, String recipient, String message) {}
