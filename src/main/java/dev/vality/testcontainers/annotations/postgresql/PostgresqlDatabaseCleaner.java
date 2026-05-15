package dev.vality.testcontainers.annotations.postgresql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlDatabaseCleaner {

    private static final String CURRENT_SCHEMA_QUERY = "SELECT schema_name FROM information_schema.schemata";
    private static final String TABLES_QUERY = "SELECT tablename FROM pg_tables " +
            "WHERE schemaname = ? AND tablename NOT LIKE 'flyway%'AND tablename NOT LIKE 'schema_version'";
    private static final String TRUNCATE_TABLE_QUERY = "TRUNCATE TABLE %s.%s CASCADE";
    private static final Set<String> EXCLUDE_SCHEMAS = Set.of("information_schema");
    private static final String PG_ = "pg_";
    private static final String SQL_ = "sql_";

    @SneakyThrows
    public static void cleanupDatabaseTables(
            String jdbcUrl,
            String username,
            String password,
            List<String> excludedTables) {
        try (var connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            for (var schema : getSchemas(connection)) {
                var tables = getUserTables(connection, schema, excludedTables);
                if (!tables.isEmpty()) {
                    truncateTables(connection, schema, tables);
                }
            }
        }
    }

    @SneakyThrows
    private static Set<String> getSchemas(Connection connection) {
        var schemas = new HashSet<String>();
        try (
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(CURRENT_SCHEMA_QUERY)) {
            while (resultSet.next()) {
                var schema = resultSet.getString("schema_name");
                if (!EXCLUDE_SCHEMAS.contains(schema)
                        && !schema.startsWith(PG_) && !schema.startsWith(SQL_)) {
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    @SneakyThrows
    private static List<String> getUserTables(Connection connection, String schema, List<String> excludedTables) {
        var tables = new ArrayList<String>();
        try (var statement = connection.prepareStatement(TABLES_QUERY)) {
            statement.setString(1, schema);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var tableName = resultSet.getString("tablename");
                    boolean isTruncatable = !tableName.startsWith(PG_)
                            && !tableName.startsWith(SQL_)
                            && !excludedTables.contains(tableName);
                    if (isTruncatable) {
                        tables.add(tableName);
                    }
                }
            }
        }
        return tables;
    }

    @SneakyThrows
    private static void truncateTables(Connection connection, String schema, List<String> tables) {
        try (var statement = connection.createStatement()) {
            statement.execute("SET session_replication_role = 'replica'");
            for (var table : tables) {
                log.debug("Truncating table: {}", table);
                statement.execute(String.format(TRUNCATE_TABLE_QUERY, schema, table));
            }
            statement.execute("SET session_replication_role = 'origin'");
        }
    }
}
