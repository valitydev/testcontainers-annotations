package dev.vality.testcontainers.annotations.postgresql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class PostgresqlContainerExtension extends PostgreSQLContainer<PostgresqlContainerExtension> {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String TAG_PROPERTY = "testcontainers.postgresql.tag";
    private static final String CURRENT_SCHEMA_QUERY = "SELECT schema_name FROM information_schema.schemata";
    private static final String TABLES_QUERY = "SELECT tablename FROM pg_tables " +
            "WHERE schemaname = ? AND tablename NOT LIKE 'flyway%'";
    private static final String TRUNCATE_TABLE_TEMPLATE = "TRUNCATE TABLE %s.%s CASCADE";
    private static final Set<String> SYSTEM_SCHEMAS = Set.of("information_schema", "public");
    private static final String PG_ = "pg_";
    private static final String SQL_ = "sql_";

    public PostgresqlContainerExtension() {
        super(DockerImageName
                .parse(POSTGRESQL_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withNetworkAliases("postgresql-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    @SneakyThrows
    public void cleanupDatabaseTables() {
        try (var connection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword())) {
            for (var schema : getSchemas(connection)) {
                var tables = getUserTables(connection, schema);
                if (!tables.isEmpty()) {
                    truncateTables(connection, schema, tables);
                }
            }
        }
    }

    @SneakyThrows
    private Set<String> getSchemas(Connection connection) {
        var schemas = new HashSet<String>();
        try (var statement = connection.createStatement(); var resultSet = statement.executeQuery(CURRENT_SCHEMA_QUERY)) {
            while (resultSet.next()) {
                var schema = resultSet.getString("schema_name");
                if (!SYSTEM_SCHEMAS.contains(schema)
                        && !schema.startsWith(PG_) && !schema.startsWith(SQL_)) {
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    @SneakyThrows
    private List<String> getUserTables(Connection connection, String schema) {
        var tables = new ArrayList<String>();
        try (var statement = connection.prepareStatement(TABLES_QUERY)) {
            statement.setString(1, schema);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var tableName = resultSet.getString("tablename");
                    if (!tableName.startsWith(PG_) && !tableName.startsWith(SQL_)) {
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
