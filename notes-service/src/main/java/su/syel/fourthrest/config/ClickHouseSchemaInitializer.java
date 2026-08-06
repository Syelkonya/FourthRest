package su.syel.fourthrest.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Создает таблицу request_events в ClickHouse при старте приложения.
 *
 * @PostConstruct — метод вызывается после создания бина и инъекции зависимостей.
 * Это гарантирует, что таблица существует до того, как приложение начнет
 * принимать запросы.
 *
 * CREATE TABLE IF NOT EXISTS — идемпотентная операция.
 * Если таблица уже есть — ничего не произойдет.
 *
 * Типы данных ClickHouse:
 *   DateTime64(3, 'UTC') — timestamp с точностью 3 знака (миллисекунды), в UTC
 *   String               — строка произвольной длины
 *   Nullable(Int64)      — целое число, которое может быть NULL
 *   Nullable(Int32)      — 32-битное целое, может быть NULL
 *   Nullable(String)     — строка, которая может быть NULL
 *
 * ENGINE = MergeTree() — основной движок ClickHouse для аналитических таблиц.
 * ORDER BY (event_time, doc_id) — определяет порядок хранения данных на диске
 *   и служит первичным ключом для ускорения запросов по этим полям.
 */
@Slf4j
@Component
public class ClickHouseSchemaInitializer {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public ClickHouseSchemaInitializer(JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        try {
            clickHouseJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS request_events (
                    event_time    DateTime64(3, 'UTC'),
                    doc_id        String,
                    event_type    String,
                    status        String,
                    processing_ms Nullable(Int64),
                    http_status   Nullable(Int32),
                    error_type    Nullable(String)
                ) ENGINE = MergeTree()
                ORDER BY (event_time, doc_id)
            """);
            log.info("ClickHouse table 'request_events' ensured");
        } catch (Exception e) {
            log.error("Failed to initialize ClickHouse schema: {}", e.getMessage());
            // Не роняем приложение — ClickHouse может быть временно недоступен.
            // Записи будут теряться, но бизнес-логика продолжит работать.
        }
    }
}