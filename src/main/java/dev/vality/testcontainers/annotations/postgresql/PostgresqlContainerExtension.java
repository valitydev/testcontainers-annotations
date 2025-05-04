package dev.vality.testcontainers.annotations.postgresql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class PostgresqlContainerExtension extends PostgreSQLContainer<PostgresqlContainerExtension> {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String TAG_PROPERTY = "testcontainers.postgresql.tag";

    private static final String CURRENT_SCHEMA_QUERY = "SELECT current_schema()";
    private static final String TABLES_QUERY = "SELECT tablename FROM pg_tables " +
            "WHERE schemaname = ? AND tablename NOT LIKE 'flyway%'";
    private static final String TRUNCATE_TABLE_TEMPLATE = "TRUNCATE TABLE %s.%s CASCADE";

    public PostgresqlContainerExtension() {
        super(DockerImageName
                .parse(POSTGRESQL_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withNetworkAliases("postgres-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    @SneakyThrows
    public void cleanupDatabaseTables() {
        try (var connection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword())) {
            var currentSchema = getCurrentSchema(connection);
            var tables = getUserTables(connection, currentSchema);
            if (!tables.isEmpty()) {
                truncateTables(connection, currentSchema, tables);
            }
        }
    }

    @SneakyThrows
    private String getCurrentSchema(Connection connection) {
        try (var statement = connection.createStatement(); var resultSet = statement.executeQuery(CURRENT_SCHEMA_QUERY)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return "public";
        }
    }

    @SneakyThrows
    private List<String> getUserTables(Connection connection, String schema) {
        var tables = new ArrayList<String>();
        try (var statement = connection.prepareStatement(TABLES_QUERY)) {
            statement.setString(1, schema);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var tableName = resultSet.getString("tablename");
                    if (!tableName.startsWith("pg_") && !tableName.startsWith("sql_")) {
                        tables.add(tableName);
                    }
                }
            }
        }
        return tables;
    }

    @SneakyThrows
    private void truncateTables(Connection connection, String schema, List<String> tables) {
        try (var statement = connection.createStatement()) {
            statement.execute("SET session_replication_role = 'replica'");
            for (var table : tables) {
                log.debug("Truncating table: {}", table);
                statement.execute(String.format(TRUNCATE_TABLE_TEMPLATE, schema, table));
            }
            statement.execute("SET session_replication_role = 'origin'");
        }
    }
}
