package su.syel.fourthrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import su.syel.fourthrest.dto.request.CreateDocumentRequest;
import su.syel.fourthrest.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DocumentIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static ClickHouseContainer clickHouseContainer =
            new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server:24.3"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MongoDB — Testcontainer сам выдаёт URL с рандомным портом
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);

        // ClickHouse — пользователь default, пароль пустой (дефолт контейнера)
        registry.add("clickhouse.url", () ->
                clickHouseContainer.getJdbcUrl() + "?user=default&password=");

        // Отключаем Eureka — в тестах нет Eureka-сервера
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JdbcTemplate clickHouseJdbcTemplate;

    @BeforeEach
    void cleanUp() {
        documentRepository.deleteAll();
        try {
            clickHouseJdbcTemplate.execute("TRUNCATE TABLE IF EXISTS request_events");
        } catch (Exception e) {
            // Таблица может ещё не существовать при первом запуске
        }
    }

    @Test
    @DisplayName("POST /api/v1/document — успешное создание документа в MongoDB")
    void shouldCreateDocumentInMongo() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Test document body",
                List.of("https://example.com", "https://test.com")
        );

        String responseBody = mockMvc.perform(post("/api/v1/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.body").value("Test document body"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Проверяем, что документ реально в MongoDB
        String docId = objectMapper.readTree(responseBody).get("id").asText();
        assertThat(documentRepository.findById(docId)).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/document — событие REQUEST_RECEIVED появляется в ClickHouse")
    void shouldRecordRequestReceivedEventInClickHouse() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Event test body",
                List.of("https://example.com")
        );

        String responseBody = mockMvc.perform(post("/api/v1/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String docId = objectMapper.readTree(responseBody).get("id").asText();

        // Ждём асинхронную запись в ClickHouse
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> events = clickHouseJdbcTemplate.queryForList(
                    "SELECT * FROM request_events WHERE doc_id = ? AND event_type = ?",
                    docId, "REQUEST_RECEIVED"
            );
            assertThat(events).hasSize(1);

            Map<String, Object> event = events.get(0);
            assertThat(event.get("status")).isEqualTo("NEW");
            assertThat(event.get("http_status")).isEqualTo(201);
            assertThat(event.get("error_type")).isNull();
        });
    }

    @Test
    @DisplayName("POST /api/v1/document — валидация: пустой body возвращает 400")
    void shouldReturnBadRequestForEmptyBody() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "",
                List.of("https://example.com")
        );

        mockMvc.perform(post("/api/v1/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/document — валидация: null links возвращает 400")
    void shouldReturnBadRequestForNullLinks() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest("body", null);

        mockMvc.perform(post("/api/v1/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Отказ ClickHouse не ломает сохранение документа в MongoDB")
    void shouldSaveDocumentEvenWhenClickHouseIsDown() throws Exception {
        // Останавливаем ClickHouse
        clickHouseContainer.stop();

        CreateDocumentRequest request = new CreateDocumentRequest(
                "ClickHouse is down body",
                List.of("https://example.com")
        );

        // Запрос всё равно проходит — MongoDB работает
        String responseBody = mockMvc.perform(post("/api/v1/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String docId = objectMapper.readTree(responseBody).get("id").asText();
        assertThat(documentRepository.findById(docId)).isPresent();

        // Перезапускаем ClickHouse для остальных тестов
        clickHouseContainer.start();
    }
}