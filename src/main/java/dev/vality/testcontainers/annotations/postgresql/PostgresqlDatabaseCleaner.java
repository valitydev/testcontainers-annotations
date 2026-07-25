package dev.vality.testcontainers.annotations.postgresql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlDatabaseCleaner {

    private static final String SCHEMAS_QUERY = "SELECT schema_name FROM information_schema.schemata";
    private static final String TABLES_QUERY = """
            SELECT tablename
            FROM pg_tables
            WHERE schemaname = ?
              AND tablename NOT LIKE 'flyway%'
              AND tablename <> 'schema_version'
            """;
    private static final Set<String> EXCLUDED_SCHEMAS = Set.of("information_schema");

    public static void cleanupDatabaseTables(
            String jdbcUrl,
            String username,
            String password,
            List<String> excludedTables) {
        try (var connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            cleanup(connection, excludedTables == null ? List.of() : excludedTables);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to clean PostgreSQL database " + jdbcUrl, ex);
        }
    }

    private static void cleanup(Connection connection, List<String> excludedTables) throws SQLException {
        var previousAutoCommit = connection.getAutoCommit();
        Throwable failure = null;
        connection.setAutoCommit(false);
        try {
            var exclusions = new HashSet<>(excludedTables);
            var tables = new ArrayList<TableReference>();
            for (var schema : getSchemas(connection)) {
                tables.addAll(getUserTables(connection, schema, exclusions));
            }
            truncateTables(connection, tables);
            connection.commit();
        } catch (SQLException | RuntimeException ex) {
            failure = ex;
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                ex.addSuppressed(rollbackException);
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException restoreException) {
                if (failure != null) {
                    failure.addSuppressed(restoreException);
                } else {
                    throw restoreException;
                }
            }
        }
    }

    private static Set<String> getSchemas(Connection connection) throws SQLException {
        var schemas = new LinkedHashSet<String>();
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(SCHEMAS_QUERY)) {
            while (resultSet.next()) {
                var schema = resultSet.getString("schema_name");
                if (!EXCLUDED_SCHEMAS.contains(schema)
                        && !schema.startsWith("pg_")
                        && !schema.startsWith("sql_")) {
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    private static List<TableReference> getUserTables(
            Connection connection,
            String schema,
            Set<String> exclusions) throws SQLException {
        var tables = new ArrayList<TableReference>();
        try (var statement = connection.prepareStatement(TABLES_QUERY)) {
            statement.setString(1, schema);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var table = resultSet.getString("tablename");
                    if (!isExcluded(schema, table, exclusions)) {
                        tables.add(new TableReference(schema, table));
                    }
                }
            }
        }
        return tables;
    }

    private static boolean isExcluded(String schema, String table, Set<String> exclusions) {
        return exclusions.contains(table) || exclusions.contains(schema + "." + table);
    }

    private static void truncateTables(Connection connection, List<TableReference> tables) throws SQLException {
        if (tables.isEmpty()) {
            return;
        }
        var qualifiedTables = tables.stream()
                .map(table -> quoteIdentifier(table.schema()) + "." + quoteIdentifier(table.table()))
                .collect(Collectors.joining(", "));
        var sql = "TRUNCATE TABLE " + qualifiedTables + " RESTART IDENTITY CASCADE";
        log.debug("Cleaning PostgreSQL tables: {}", qualifiedTables);
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private record TableReference(String schema, String table) {
    }
}
