package su.ternovskii.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import su.ternovskii.notificationservice.model.Channel;
import su.ternovskii.notificationservice.model.DeliveryStatus;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "channel_delivery")
public class ChannelDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @Enumerated(EnumType.STRING)
    private Channel channel;           // SMS, PUSH, EMAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;     // PENDING, SENT, FAILED

    @Column(nullable = false)
    private int retryCount = 0;

    private Instant nextRetryAt;       // когда делать следующий retry (null = не нужен)
}