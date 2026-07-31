package su.ternovskii.notificationservice.dto.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.ternovskii.notificationservice.dto.kafka.NotificationResult;
import su.ternovskii.notificationservice.entity.ChannelDeliveryEntity;
import su.ternovskii.notificationservice.kafka.EventPublisher;
import su.ternovskii.notificationservice.model.Channel;
import su.ternovskii.notificationservice.model.DeliveryStatus;
import su.ternovskii.notificationservice.repository.ChannelDeliveryRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultListener {

    private final ChannelDeliveryRepository channelDeliveryRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    @KafkaListener(topics = "notification.result", groupId = "notification-service")
    public void handleResult(NotificationResult result) {
        Channel channel = Channel.valueOf(result.channel());

        ChannelDeliveryEntity delivery = channelDeliveryRepository
                .findByNotificationIdAndChannel(result.notificationId(), channel)
                .orElse(null);

        if (delivery == null) {
            log.warn("ChannelDelivery not found: notificationId={}, channel={}",
                    result.notificationId(), result.channel());
            return;
        }

        // ИДЕМПОТЕНТНОСТЬ: если этот канал уже SENT — игнорируем дубликат результата
        if (delivery.getStatus() == DeliveryStatus.SENT) {
            log.info("Duplicate result ignored: notificationId={}, channel={} already SENT",
                    result.notificationId(), result.channel());
            return;
        }

        if (result.success()) {
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setNextRetryAt(null);
            eventPublisher.publish("DELIVERY_SUCCESS", result.notificationId(),
                    result.channel(), delivery.getNotification().getRecipient(),
                    delivery.getRetryCount(), null, null);
        } else {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setRetryCount(delivery.getRetryCount() + 1);
            delivery.setNextRetryAt(Instant.now().plusSeconds(60));
            eventPublisher.publish("DELIVERY_FAILED", result.notificationId(),
                    result.channel(), delivery.getNotification().getRecipient(),
                    delivery.getRetryCount(), null, result.errorMessage());
        }

        channelDeliveryRepository.save(delivery);
    }
}