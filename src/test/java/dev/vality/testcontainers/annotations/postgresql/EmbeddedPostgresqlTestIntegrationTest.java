package dev.vality.testcontainers.annotations.postgresql;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedPostgresqlTest
@SpringBootTest(classes = EmbeddedPostgresqlTestIntegrationTest.Config.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmbeddedPostgresqlTestIntegrationTest {

    private static final String TABLE_NAME = "embedded_postgresql_test";

    @Autowired
    private Environment environment;

    @Test
    @Order(1)
    void shouldStartEmbeddedPostgresqlAndExposeDatasourceProperties() throws Exception {
        assertThat(environment.getRequiredProperty("spring.datasource.url"))
                .startsWith("jdbc:postgresql://localhost:");
        assertThat(environment.getRequiredProperty("spring.datasource.username"))
                .isEqualTo("postgres");
        assertThat(environment.getRequiredProperty("spring.datasource.password"))
                .isEmpty();
        assertThat(environment.getRequiredProperty("postgres.db.url"))
                .isEqualTo(environment.getRequiredProperty("spring.datasource.url"));

        try (var connection = connection()) {
            execute(connection, "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id INT PRIMARY KEY)");
            execute(connection, "INSERT INTO " + TABLE_NAME + " (id) VALUES (1)");

            assertThat(countRows(connection))
                    .isEqualTo(1);
        }
    }

    @Test
    @Order(2)
    void shouldCleanupTablesBeforeEachTest() throws Exception {
        try (var connection = connection()) {
            assertThat(countRows(connection))
                    .isZero();
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                environment.getRequiredProperty("spring.datasource.url"),
                environment.getRequiredProperty("spring.datasource.username"),
                environment.getRequiredProperty("spring.datasource.password"));
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int countRows(Connection connection) throws Exception {
        try (
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + TABLE_NAME)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Configuration
    static class Config {
    }
}
