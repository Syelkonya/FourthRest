package su.syel.fourthrest.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Декоратор, который «протаскивает» MDC-контекст из вызывающего потока
 * в поток исполнителя (executor).
 *
 * Без этого декоратора:
 *   HTTP-поток:  MDC = {requestId: "abc"}
 *   Async-поток: MDC = {}  ← requestId потерян
 *
 * С декоратором:
 *   HTTP-поток:  MDC = {requestId: "abc"}  → захватили карту
 *   Async-поток: MDC = {requestId: "abc"}  ← восстановили из захваченной карты
 *
 * Механизм:
 * 1. decorate() вызывается в HTTP-потоке — мы берем копию MDC.
 * 2. Возвращаем новый Runnable, который перед выполнением задачи
 *    устанавливает сохраненную MDC-карту.
 * 3. После выполнения — очищаем MDC, чтобы не загрязнить поток для следующей задачи.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Вызывается в ВЫЗЫВАЮЩЕМ потоке (HTTP-поток)
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // Выполняется в ASYNC-потоке
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}