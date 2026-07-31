package su.ternovskii.pushadapter.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import su.ternovskii.pushadapter.dto.NotificationCommand;
import su.ternovskii.pushadapter.dto.NotificationResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushListener {

    private final KafkaTemplate<String, NotificationResult> kafkaTemplate;
    
    @KafkaListener(topics = "notification.push.send", groupId = "push-adapter")
    public void handle(NotificationCommand command, Acknowledgment ack) {
        log.info("PUSH adapter received: notificationId={}, recipient={}",
                command.notificationId(), command.recipient());

        // Эмуляция отправки PUSH (всегда успех для PUSH)
        boolean success = true;
        String error = null;

        log.info("PUSH sent successfully for notificationId={}", command.notificationId());

        // Отправляем результат обратно в Kafka
        NotificationResult result = new NotificationResult(
                command.notificationId(),
                "PUSH",
                success,
                error
        );
        kafkaTemplate.send("notification.result",
                String.valueOf(command.notificationId()), result);

        log.info("PUSH result sent to notification.result: success={}", success);
        ack.acknowledge();
    }
}
