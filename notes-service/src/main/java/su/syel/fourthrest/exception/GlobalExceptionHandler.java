package su.syel.fourthrest.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su.syel.fourthrest.model.EventType;
import su.syel.fourthrest.service.ClickHouseEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ClickHouseEventService clickHouseEventService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorFieldsResponseDTO> handleParameterValidation(
            MethodArgumentNotValidException e) {

        BindingResult bindingResult = e.getBindingResult();
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrorList = bindingResult.getFieldErrors();

            for (FieldError fieldError : fieldErrorList) {
                String field = fieldError.getField();
                String message = fieldError.getDefaultMessage();
                errors.add(new FieldErrorDetail(field, message));
            }
        }

        clickHouseEventService.recordEvent(
                "",
                EventType.REQUEST_FAILED,
                null,
                null,
                HttpStatus.BAD_REQUEST.value(),
                "ValidationError"
        );

        ErrorFieldsResponseDTO errorFieldsResponseDTO = new ErrorFieldsResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                errors,
                "There is NO some mandatory fields"
        );

        return ResponseEntity.badRequest().body(errorFieldsResponseDTO);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatus(
            ResponseStatusException e) {

        HttpStatusCode statusCode = e.getStatusCode();
        String message = e.getReason();
        if (message == null) {
            message = e.getMessage();
        }

        clickHouseEventService.recordEvent(
                "",
                EventType.REQUEST_FAILED,
                null,
                null,
                statusCode.value(),
                e.getClass().getSimpleName()
        );

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                LocalDateTime.now(),
                statusCode.value(),
                message
        );

        return ResponseEntity.status(statusCode).body(errorResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralException(Exception e) {

        log.info(e.getMessage());

        clickHouseEventService.recordEvent(
                "",
                EventType.REQUEST_FAILED,
                null,
                null,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.getClass().getSimpleName()
        );

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDTO);
    }
}