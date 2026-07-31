package su.ternovskii.notificationservice.model;

public enum DeliveryStatus {
    PENDING,   // отправлено в Kafka, ждём ответа
    SENT,      // адаптер подтвердил доставку
    FAILED     // адаптер сообщил об ошибке, ждём retry
}