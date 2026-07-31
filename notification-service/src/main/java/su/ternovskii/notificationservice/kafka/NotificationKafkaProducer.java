package su.ternovskii.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import su.ternovskii.notificationservice.dto.kafka.NotificationCommand;
import su.ternovskii.notificationservice.model.Channel;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, NotificationCommand> kafkaTemplate;

    public void sendCommand(NotificationCommand command) {
        String topic = resolveTopicName(command.channel());
        String key = String.valueOf(command.notificationId());

        log.info("Sending command to topic={}, notificationId={}", topic, command.notificationId());

        kafkaTemplate.send(topic, key, command);
    }

    private String resolveTopicName(String channel) {
        return switch (channel) {
            case "SMS"   -> "notification.sms.send";
            case "PUSH"  -> "notification.push.send";
            case "EMAIL" -> "notification.email.send";
            default -> throw new IllegalStateException("Unexpected value: " + channel);
        };
    }
}