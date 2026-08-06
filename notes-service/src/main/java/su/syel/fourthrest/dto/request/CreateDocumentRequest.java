package su.syel.fourthrest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO для входящего запроса на создание документа.
 *
 * @NotBlank — строка не null, не пустая, не состоит только из пробелов.
 * @NotNull  — значение не null.
 * @Size     — ограничение на размер коллекции.
 *
 * Когда Spring получает JSON в теле запроса, он десериализует его в этот объект
 * через Jackson. Затем, если на аргументе контроллера стоит @Valid,
 * Spring запускает валидацию. Если какое-то ограничение нарушено,
 * выбрасывается MethodArgumentNotValidException (HTTP 400).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateDocumentRequest {

    @NotBlank(message = "body не должен быть пустым")
    private String body;

    @NotNull(message = "links не должен быть null")
    @Size(min = 1, message = "links должен содержать хотя бы одну ссылку")
    private List<String> links;
}