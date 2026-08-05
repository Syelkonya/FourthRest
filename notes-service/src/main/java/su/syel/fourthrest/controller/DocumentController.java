package su.syel.fourthrest.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import su.syel.fourthrest.dto.request.CreateDocumentRequest;
import su.syel.fourthrest.dto.response.DocumentResponse;
import su.syel.fourthrest.mapper.DocumentMapper;
import su.syel.fourthrest.model.DocumentEntity;
import su.syel.fourthrest.model.DocumentStatus;
import su.syel.fourthrest.model.EventType;
import su.syel.fourthrest.service.ClickHouseEventService;
import su.syel.fourthrest.service.DocumentService;

/**
 * REST-контроллер для работы с документами.
 *
 * @RestController = @Controller + @ResponseBody
 *   Все методы автоматически сериализуют возвращаемый объект в JSON.
 *
 * @RequestMapping("/api/v1") — общий префикс для всех эндпоинтов контроллера.
 *
 * @Valid — запускает валидацию на объекте CreateDocumentRequest.
 *   Если body пустой или links null — Spring выбросит MethodArgumentNotValidException
 *   ДО входа в метод. Это обрабатывается в GlobalExceptionHandler.
 *
 * Логика:
 * 1. Замеряем время начала (из фильтра или вручную).
 * 2. Создаем документ через DocumentService.
 * 3. Асинхронно записываем REQUEST_RECEIVED в ClickHouse.
 * 4. Возвращаем 201 Created с телом документа.
 *
 * При ошибке — ловим в catch или в GlobalExceptionHandler,
 * записываем REQUEST_FAILED.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final ClickHouseEventService clickHouseEventService;
    private final DocumentMapper documentMapper;

    @PostMapping("/document")
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            HttpServletRequest httpRequest) {

        long startTime = getStartTime(httpRequest);

        try {
            // 1. Создаем документ в MongoDB
            DocumentEntity documentEntity = documentService.createDocument(request);

            long processingMs = System.currentTimeMillis() - startTime;

            // 2. Асинхронно записываем событие (не блокирует ответ клиенту)
            clickHouseEventService.recordEvent(
                    documentEntity.getId(),
                    EventType.REQUEST_RECEIVED,
                    DocumentStatus.NEW.name(),
                    processingMs,
                    HttpStatus.CREATED.value(),  // 201
                    null
            );

            log.info("Document created: id={}, processingMs={}", documentEntity.getId(), processingMs);

            // 3. Возвращаем 201 Created
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(documentMapper.toResponseDTO(documentEntity));

        } catch (Exception e) {
            long processingMs = System.currentTimeMillis() - startTime;

            // Записываем ошибку в ClickHouse (асинхронно)
            clickHouseEventService.recordEvent(
                    "",
                    EventType.REQUEST_FAILED,
                    null,
                    processingMs,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e.getClass().getSimpleName()
            );

            throw e; // Пробрасываем для стандартной обработки Spring
        }
    }

    /**
     * Получает время начала запроса из атрибута, установленного фильтром.
     * Если атрибута нет — берет текущее время.
     */
    private long getStartTime(HttpServletRequest request) {
        Object startTimeAttr = request.getAttribute("requestStartTime");
        if (startTimeAttr instanceof Long) {
            return (Long) startTimeAttr;
        }
        return System.currentTimeMillis();
    }
}