package su.syel.fourthrest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import su.syel.fourthrest.dto.request.CreateDocumentRequest;
import su.syel.fourthrest.model.DocumentEntity;
import su.syel.fourthrest.model.DocumentStatus;
import su.syel.fourthrest.repository.DocumentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Бизнес-логика работы с документами.
 *
 * Использует два инструмента для работы с MongoDB:
 *
 * 1. DocumentRepository (MongoRepository) — для простых CRUD-операций:
 *    save(), findById(), и т.д.
 *
 * 2. MongoTemplate — для сложных атомарных операций:
 *    findAndModify() для безопасного захвата документов в джобе.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Создает новый документ в MongoDB.
     *
     * 1. Создаем DocumentEntity с status=NEW и timestamps.
     * 2. repository.save() вставляет документ в MongoDB.
     *    Поскольку id=null, MongoDB генерирует ObjectId автоматически.
     * 3. После save() поле id заполнено — MongoDB вернул сгенерированный _id.
     */
    public DocumentEntity createDocument(CreateDocumentRequest request) {
        DocumentEntity entity = new DocumentEntity(request.getBody(), request.getLinks());
        DocumentEntity saved = documentRepository.save(entity);
        log.info("Document created with id={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    /**
     * Атомарно захватывает до batchSize документов со статусом NEW,
     * переводя их в PROCESSING.
     *
     * findAndModify — атомарная операция MongoDB:
     * - Находит ОДИН документ, соответствующий Query
     * - Применяет Update
     * - Возвращает документ (до или после изменения, в зависимости от опций)
     * - Всё это в одной атомарной операции
     *
     * FindAndModifyOptions.options().returnNew(true) — вернуть документ
     * ПОСЛЕ применения Update (с новым статусом PROCESSING).
     *
     * Цикл: вызываем findAndModify пока есть документы со статусом NEW
     * или пока не набрали batchSize штук.
     *
     * Почему это безопасно:
     * Даже если два потока одновременно вызовут findAndModify с одинаковым
     * условием (status=NEW), MongoDB гарантирует, что каждый документ
     * будет возвращен только ОДНОМУ из них. Второй получит другой документ
     * или null.
     */
    public List<DocumentEntity> claimNewDocuments(int batchSize) {
        List<DocumentEntity> claimed = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            DocumentEntity doc = mongoTemplate.findAndModify(
                    Query.query(Criteria.where("status").is(DocumentStatus.NEW)),
                    new Update()
                            .set("status", DocumentStatus.PROCESSING)
                            .set("updatedAt", Instant.now()),
                    FindAndModifyOptions.options().returnNew(true),
                    DocumentEntity.class
            );

            if (doc == null) {
                // Больше нет документов со статусом NEW
                break;
            }

            claimed.add(doc);
        }

        return claimed;
    }

    /**
     * Переводит документ из PROCESSING в PROCESSED.
     */
    public void markProcessed(DocumentEntity document) {
        document.setStatus(DocumentStatus.PROCESSED);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        log.info("Document {} marked as PROCESSED", document.getId());
    }
}