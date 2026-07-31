package su.ternovskii.notificationservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import su.ternovskii.notificationservice.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final int MAX_RETRIES = 5;

    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 30_000)
    public void retryPendingNotifications() {
        notificationService.retryFailedDeliveries(MAX_RETRIES);
    }
}