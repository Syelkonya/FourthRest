package su.syel.fourthrest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Конфигурация асинхронных исполнителей (thread pools).
 *
 * @EnableAsync      — включает поддержку @Async-методов.
 * @EnableScheduling — включает поддержку @Scheduled-методов.
 *
 * Мы создаем два пула потоков:
 *
 * 1) clickHouseExecutor — для асинхронной записи событий в ClickHouse.
 *    Используется аннотацией @Async("clickHouseExecutor").
 *    Имеет MdcTaskDecorator для передачи requestId в async-потоки.
 *
 * 2) batchProcessingExecutor — для обработки батчей документов.
 *    Каждый батч из 5 документов обрабатывается в отдельном потоке.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Пул потоков для записи событий в ClickHouse.
     *
     * corePoolSize=2     — 2 потока живут постоянно.
     * maxPoolSize=5      — при нагрузке может вырасти до 5.
     * queueCapacity=200  — до 200 задач в очереди ожидания.
     * taskDecorator       — MdcTaskDecorator для передачи requestId.
     * rejectedHandler     — CallerRunsPolicy: если пул переполнен,
     *                       задача выполнится в вызывающем потоке
     *                       (лучше, чем потерять событие).
     */
    @Bean("clickHouseExecutor")
    public Executor clickHouseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ch-event-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Пул потоков для обработки батчей документов в джобе.
     *
     * corePoolSize=3  — 3 батча могут обрабатываться параллельно.
     * maxPoolSize=10  — при большом количестве документов до 10 параллельных батчей.
     * queueCapacity=50 — до 50 батчей в очереди.
     */
    @Bean("batchProcessingExecutor")
    public Executor batchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-proc-");
        executor.initialize();
        return executor;
    }
}