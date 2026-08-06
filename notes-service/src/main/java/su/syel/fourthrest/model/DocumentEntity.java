package su.syel.fourthrest.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB-документ в коллекции "documents".
 *
 * @Document — помечает класс как документ MongoDB.
 * @Id       — поле, которое станет _id в MongoDB.
 *             Если тип String и значение null при save() — MongoDB сгенерирует ObjectId.
 * @Indexed  — создает индекс на поле status, чтобы запросы по статусу были быстрыми.
 */
@Setter
@Getter
@NoArgsConstructor
@Document(collection = "documents")
public class DocumentEntity {

    @Id
    private String id;

    private String body;

    private List<String> links;

    @Indexed // Индекс для быстрого поиска по статусу в джобе
    private DocumentStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    public DocumentEntity(String body, List<String> links) {
        this.body = body;
        this.links = links;
        this.status = DocumentStatus.NEW;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}