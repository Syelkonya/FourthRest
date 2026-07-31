package su.ternovskii.emailadapter.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import su.ternovskii.emailadapter.dto.NotificationCommand;
import su.ternovskii.emailadapter.dto.NotificationResult;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    private final KafkaTemplate<String, NotificationResult> kafkaTemplate;
    private final Random random = new Random();

    @KafkaListener(topics = "notification.email.send", groupId = "email-adapter")
    public void handle(NotificationCommand command, Acknowledgment ack) {
        log.info("Email adapter received: notificationId={}", command.notificationId());

        // Эмуляция: 20% сообщений «не доставлены»
        boolean success = random.nextInt(100) >= 20;  // 80% успех, 20% неудача
        String error = null;

        if (success) {
            log.info("Email sent successfully for notificationId={}", command.notificationId());
        } else {
            error = "Email gateway unavailable (simulated failure)";
            log.warn("Email FAILED for notificationId={}: {}", command.notificationId(), error);
        }

        NotificationResult result = new NotificationResult(
                command.notificationId(), "EMAIL", success, error);
        kafkaTemplate.send("notification.result",
                String.valueOf(command.notificationId()), result);

        ack.acknowledge();
    }
}
