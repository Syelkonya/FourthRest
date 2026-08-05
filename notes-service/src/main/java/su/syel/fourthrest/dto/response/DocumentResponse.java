package su.syel.fourthrest.dto.response;


import lombok.Getter;
import su.syel.fourthrest.model.DocumentEntity;
import su.syel.fourthrest.model.DocumentStatus;

import java.time.Instant;
import java.util.List;

/**
 * DTO для ответа клиенту.
 * Отделяем внутреннюю модель (DocumentEntity) от API-контракта.
 * Это позволяет менять структуру БД без изменения API.
 */
@Getter
public class DocumentResponse {

    private String id;
    private String body;
    private List<String> links;
    private DocumentStatus status;
    private Instant createdAt;
    private Instant updatedAt;

}