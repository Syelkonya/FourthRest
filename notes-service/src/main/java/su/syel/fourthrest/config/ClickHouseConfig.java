package su.syel.fourthrest.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Конфигурация подключения к ClickHouse.
 *
 * Мы НЕ используем Spring Boot auto-configuration для DataSource,
 * потому что основная БД — MongoDB, а не SQL. Поэтому создаем
 * DataSource и JdbcTemplate вручную.
 *
 * JdbcTemplate — удобная обертка Spring над JDBC. Она:
 * - Управляет соединениями (открытие/закрытие)
 * - Обрабатывает исключения (превращает checked SQLException в unchecked DataAccessException)
 * - Поддерживает параметризованные запросы (защита от SQL-инъекций)
 *
 * @Value("${clickhouse.url}") — инъекция значения из application.yml.
 */
@Slf4j
@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.url}")
    private String clickHouseUrl;

    @Bean
    public DataSource clickHouseDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setUrl(clickHouseUrl);
        return dataSource;
    }

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }
}