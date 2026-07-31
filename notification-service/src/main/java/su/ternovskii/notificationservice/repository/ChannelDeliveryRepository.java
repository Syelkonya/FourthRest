package su.ternovskii.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.ternovskii.notificationservice.entity.ChannelDeliveryEntity;
import su.ternovskii.notificationservice.model.Channel;
import su.ternovskii.notificationservice.model.DeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChannelDeliveryRepository extends JpaRepository<ChannelDeliveryEntity, Long> {

    // Для обработки результата: найти запись по notificationId + channel
    Optional<ChannelDeliveryEntity> findByNotificationIdAndChannel(Long notificationId, Channel channel);

    // Для Scheduler: найти все FAILED записи, у которых пришло время retry
    List<ChannelDeliveryEntity> findByStatusAndNextRetryAtBeforeAndRetryCountLessThan(
            DeliveryStatus status, Instant now, int maxRetries);
}