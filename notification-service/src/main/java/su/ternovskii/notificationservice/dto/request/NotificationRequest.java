package su.ternovskii.notificationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import su.ternovskii.notificationservice.model.Channel;

public record NotificationRequest(
        Channel channel,
        @NotNull String recipient,
        @NotNull String message) {
}