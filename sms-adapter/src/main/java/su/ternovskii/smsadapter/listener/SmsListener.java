package su.ternovskii.smsadapter.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import su.ternovskii.smsadapter.dto.NotificationCommand;
import su.ternovskii.smsadapter.dto.NotificationResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsListener {

    private final KafkaTemplate<String, NotificationResult> kafkaTemplate;

    @KafkaListener(topics = "notification.sms.send", groupId = "sms-adapter")
    public void handle(NotificationCommand command, Acknowledgment ack) {
        log.info("SMS adapter received: notificationId={}, recipient={}",
                command.notificationId(), command.recipient());

        // Эмуляция отправки SMS (всегда успех для SMS)
        boolean success = true;
        String error = null;

        log.info("SMS sent successfully for notificationId={}", command.notificationId());

        // Отправляем результат обратно в Kafka
        NotificationResult result = new NotificationResult(
                command.notificationId(),
                "SMS",
                success,
                error
        );
        kafkaTemplate.send("notification.result",
                String.valueOf(command.notificationId()), result);

        log.info("SMS result sent to notification.result: success={}", success);

        ack.acknowledge();
    }
}