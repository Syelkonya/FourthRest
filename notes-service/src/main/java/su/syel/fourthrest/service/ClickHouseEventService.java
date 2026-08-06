package su.syel.fourthrest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import su.syel.fourthrest.model.EventType;

import java.time.Instant;

/**
 * Сервис для асинхронной записи событий в ClickHouse.
 *
 * Ключевые решения:
 *
 * 1. @Async("clickHouseExecutor") — метод выполняется в отдельном потоке
 *    из пула clickHouseExecutor. Вызывающий метод (контроллер/джоба)
 *    НЕ ждет завершения записи в ClickHouse.
 *
 * 2. try-catch внутри метода — если ClickHouse недоступен, исключение
 *    логируется, но НЕ пробрасывается. Это критически важно:
 *    недоступный ClickHouse не должен ломать ответ клиенту.
 *
 * 3. Сервис в отдельном бине — это обязательно для работы @Async.
 *    Spring AOP создает прокси, который перехватывает вызов и отправляет
 *    его в executor. Если вызвать @Async-метод из того же класса
 *    через this.method(), прокси не сработает.
 *
 * 4. MDC-контекст (requestId) передается через MdcTaskDecorator,
 *    настроенный на clickHouseExecutor. В логах async-потока
 *    будет виден requestId оригинального HTTP-запроса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickHouseEventService {

    private static final String INSERT_SQL = """
        INSERT INTO request_events
            (event_time, doc_id, event_type, status, processing_ms, http_status, error_type)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

    private final JdbcTemplate clickHouseJdbcTemplate;

    /**
     * Асинхронно записывает событие в ClickHouse.
     *
     * @param docId        ID документа (может быть пустой строкой, если документ не создан)
     * @param eventType    тип события
     * @param status       статус документа (может быть null)
     * @param processingMs длительность обработки в мс (может быть null)
     * @param httpStatus   HTTP-код ответа (может быть null)
     * @param errorType    тип ошибки (может быть null)
     */
    @Async("clickHouseExecutor")
    public void recordEvent(String docId,
                            EventType eventType,
                            String status,
                            Long processingMs,
                            Integer httpStatus,
                            String errorType) {
        try {
            clickHouseJdbcTemplate.update(INSERT_SQL,
                    Instant.now(),
                    docId != null ? docId : "",
                    eventType.name(),
                    status != null ? status : "",
                    processingMs,
                    httpStatus,
                    errorType
            );
            log.info("Event {} recorded for doc {}", eventType, docId);
        } catch (Exception e) {
            // КРИТИЧНО: не пробрасываем исключение!
            // ClickHouse может быть недоступен — это не должно ломать бизнес-логику.
            log.error("Failed to record event {} for doc {}: {}",
                    eventType, docId, e.getMessage());
        }
    }
}