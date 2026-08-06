package su.syel.fourthrest.job;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import su.syel.fourthrest.model.DocumentEntity;
import su.syel.fourthrest.model.DocumentStatus;
import su.syel.fourthrest.model.EventType;
import su.syel.fourthrest.service.ClickHouseEventService;
import su.syel.fourthrest.service.DocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Джоба, которая раз в минуту обрабатывает документы со статусом NEW.
 *
 * Алгоритм:
 * 1. Атомарно захватываем все документы NEW → PROCESSING (findAndModify).
 * 2. Разбиваем захваченные документы на батчи по 5.
 * 3. Каждый батч отправляем в batchProcessingExecutor — отдельный поток.
 * 4. В потоке: для каждого документа меняем PROCESSING → PROCESSED
 *    и отправляем событие STATUS_CHANGED в ClickHouse (асинхронно).
 *
 * Защита от повторной обработки:
 * - findAndModify атомарно меняет статус с NEW на PROCESSING.
 * - Документ в статусе PROCESSING не будет выбран повторно,
 *   потому что запрос ищет только status=NEW.
 * - Даже если джоба запустится снова, пока предыдущая не завершилась,
 *   те же документы не будут захвачены дважды.
 *
 * @Scheduled(fixedRate = 60000) — запуск каждые 60 секунд от начала предыдущего.
 * fixedRate vs fixedDelay:
 *   fixedRate:  |--run--|----wait----|--run--|  (60с от начала до начала)
 *   fixedDelay: |--run--|     60с     |--run--|  (60с от конца до начала)
 */
@Component
@Slf4j
public class DocumentProcessingJob {

    private static final int BATCH_SIZE = 5;

    private final DocumentService documentService;
    private final ClickHouseEventService clickHouseEventService;
    private final Executor batchProcessingExecutor;

    public DocumentProcessingJob(DocumentService documentService,
                                 ClickHouseEventService clickHouseEventService,
                                 @Qualifier("batchProcessingExecutor") Executor batchProcessingExecutor) {
        this.documentService = documentService;
        this.clickHouseEventService = clickHouseEventService;
        this.batchProcessingExecutor = batchProcessingExecutor;
    }

    @Scheduled(fixedRate = 60_000)
    public void processNewDocuments() {
        log.info("Processing job started");

        // 1. Захватываем ВСЕ документы со статусом NEW (атомарно, по одному)
        List<DocumentEntity> allClaimed = new ArrayList<>();
        List<DocumentEntity> batch;

        do {
            batch = documentService.claimNewDocuments(BATCH_SIZE);
            allClaimed.addAll(batch);
        } while (batch.size() == BATCH_SIZE);
        // Продолжаем, пока получаем полные батчи — значит, ещё есть документы

        if (allClaimed.isEmpty()) {
            log.info("No NEW documents to process");
            return;
        }

        log.info("Claimed {} documents for processing", allClaimed.size());

        // 2. Разбиваем на батчи по BATCH_SIZE
        List<List<DocumentEntity>> batches = partition(allClaimed, BATCH_SIZE);

        // 3. Каждый батч — в отдельный поток
        for (List<DocumentEntity> currentBatch : batches) {
            batchProcessingExecutor.execute(() -> processBatch(currentBatch));
        }
    }

    /**
     * Обрабатывает один батч документов.
     * Для каждого документа:
     * 1. Меняет статус PROCESSING → PROCESSED.
     * 2. Записывает событие STATUS_CHANGED в ClickHouse (асинхронно).
     */
    private void processBatch(List<DocumentEntity> batch) {
        log.info("Processing batch of {} documents in thread {}",
                batch.size(), Thread.currentThread().getName());

        for (DocumentEntity doc : batch) {
            try {
                documentService.markProcessed(doc);

                // Асинхронная запись — джоба НЕ ждет ClickHouse
                clickHouseEventService.recordEvent(
                        doc.getId(),
                        EventType.STATUS_CHANGED,
                        DocumentStatus.PROCESSED.name(),
                        null,   // processing_ms не применимо
                        null,   // http_status не применимо
                        null    // error_type — нет ошибки
                );
            } catch (Exception e) {
                log.error("Failed to process document {}: {}", doc.getId(), e.getMessage());
                // Документ останется в статусе PROCESSING.
                // В продакшене нужен механизм recovery (timeout-based retry).
            }
        }
    }

    /**
     * Разбивает список на подсписки заданного размера.
     * Пример: partition([1,2,3,4,5,6,7], 5) → [[1,2,3,4,5], [6,7]]
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}