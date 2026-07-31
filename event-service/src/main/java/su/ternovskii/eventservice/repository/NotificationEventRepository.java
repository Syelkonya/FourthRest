package su.ternovskii.eventservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.ternovskii.eventservice.entity.NotificationEventEntity;

import java.util.List;

public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, Long> {
    List<NotificationEventEntity> findByNotificationIdOrderByEventTimestamp(Long notificationId);
}
