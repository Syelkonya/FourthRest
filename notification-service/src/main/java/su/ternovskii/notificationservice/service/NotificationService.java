package su.ternovskii.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.ternovskii.notificationservice.dispatcher.NotificationDispatcher;
import su.ternovskii.notificationservice.dto.kafka.NotificationCommand;
import su.ternovskii.notificationservice.dto.kafka.NotificationResult;
import su.ternovskii.notificationservice.dto.request.NotificationRequest;
import su.ternovskii.notificationservice.dto.response.NotificationResponse;
import su.ternovskii.notificationservice.entity.ChannelDeliveryEntity;
import su.ternovskii.notificationservice.entity.NotificationEntity;
import su.ternovskii.notificationservice.entity.NotificationTemplateEntity;
import su.ternovskii.notificationservice.kafka.EventPublisher;
import su.ternovskii.notificationservice.kafka.NotificationKafkaProducer;
import su.ternovskii.notificationservice.mapper.NotificationMapper;
import su.ternovskii.notificationservice.model.Channel;
import su.ternovskii.notificationservice.model.DeliveryStatus;
import su.ternovskii.notificationservice.model.NotificationStatus;
import su.ternovskii.notificationservice.persistence.NotificationPersistence;
import su.ternovskii.notificationservice.repository.ChannelDeliveryRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {


    private final NotificationMapper notificationMapper;
    private final NotificationPersistence notificationPersistence;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationKafkaProducer notificationKafkaProducer;
    private final ChannelDeliveryRepository channelDeliveryRepository;
    private final KafkaTemplate<String, NotificationCommand> kafkaTemplate;
    private final EventPublisher eventPublisher;

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest notificationRequest) {
        // 1. Создаём уведомление в БД (как раньше)
        NotificationEntity entity = notificationPersistence.create(notificationRequest);
        log.info("Created notification id={} status=NEW", entity.getId());
        eventPublisher.publish("NOTIFICATION_CREATED", entity.getId(), null,
                entity.getRecipient(), 0, null, null);

        // 2. Для КАЖДОГО канала — создаём ChannelDelivery и шлём команду в Kafka
        for (Channel channel : Channel.values()) {
            // Создаём запись в БД: «по этому каналу статус PENDING»
            ChannelDeliveryEntity delivery = new ChannelDeliveryEntity();
            delivery.setNotification(entity);
            delivery.setChannel(channel);
            delivery.setStatus(DeliveryStatus.PENDING);
            channelDeliveryRepository.save(delivery);

            // Формируем команду для адаптера
            NotificationCommand command = new NotificationCommand(
                    entity.getId(),
                    channel.name(),
                    entity.getRecipient(),
                    entity.getMessage()
            );

            // Определяем топик по каналу
            String topic = "notification." + channel.name().toLowerCase() + ".send";

            // Отправляем в Kafka
            kafkaTemplate.send(topic, String.valueOf(entity.getId()), command);
            log.info("Sent command to {} for notificationId={}", topic, entity.getId());
            eventPublisher.publish("COMMAND_SENT", entity.getId(), channel.name(),
                    entity.getRecipient(), 0, null, null);
        }

        return notificationMapper.toResponse(entity);
    }

    public NotificationResponse getNotificationResponse(Long id) {
        NotificationEntity notificationEntity = notificationPersistence.get(id);
        return notificationMapper.toResponse(notificationEntity);
    }

    public List<NotificationResponse> getAll() {
        List<NotificationEntity> notifications = notificationPersistence.findAll();
        return notificationMapper.toResponseList(notifications);
    }

    public List<NotificationResponse> getNotificationResponseListByRecipientAndCreatedAtDesc(String recipient) {
        List<NotificationEntity> notifications = notificationPersistence.findByRecipientOrderByCreatedAtDesc(recipient);
        return notificationMapper.toResponseList(notifications);
    }

    @Transactional
    public void retryFailedDeliveries(int maxRetries) {

        List<ChannelDeliveryEntity> toRetry = channelDeliveryRepository
                .findByStatusAndNextRetryAtBeforeAndRetryCountLessThan(
                        DeliveryStatus.FAILED, Instant.now(), maxRetries);

        log.info("Retry scheduler: found {} failed deliveries", toRetry.size());

        for (ChannelDeliveryEntity delivery : toRetry) {
            eventPublisher.publish("RETRY_SCHEDULED", delivery.getNotification().getId(),
                    delivery.getChannel().name(), delivery.getNotification().getRecipient(),
                    delivery.getRetryCount(), null, null);

            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            channelDeliveryRepository.save(delivery);

            NotificationCommand command = new NotificationCommand(
                    delivery.getNotification().getId(),
                    delivery.getChannel().name(),
                    delivery.getNotification().getRecipient(),
                    delivery.getNotification().getMessage()
            );

            String topic = "notification." + delivery.getChannel().name().toLowerCase() + ".send";
            kafkaTemplate.send(topic, String.valueOf(delivery.getNotification().getId()), command);

            log.info("Retry sent to {} for notificationId={}", topic, delivery.getNotification().getId());
        }
    }
}
