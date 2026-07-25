package dev.vality.testcontainers.annotations.postgresql;

import dev.vality.testcontainers.annotations.util.TestExecutionLock;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EmbeddedPostgresqlTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final ConcurrentMap<Class<?>, EmbeddedPostgresql> INSTANCES = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        findAnnotation(testClass).ifPresent(annotation -> getOrStart(testClass, annotation));
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        findAnnotation(testClass).ifPresent(annotation -> {
            if (annotation.truncateTables()) {
                TestExecutionLock.acquire(context);
                try {
                    var postgresql = getOrStart(testClass, annotation);
                    PostgresqlDatabaseCleaner.cleanupDatabaseTables(
                            postgresql.jdbcUrl(),
                            annotation.username(),
                            annotation.password(),
                            List.copyOf(Arrays.asList(annotation.excludeTruncateTables())));
                } catch (RuntimeException ex) {
                    TestExecutionLock.release(context);
                    throw ex;
                }
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var postgresql = INSTANCES.remove(context.getRequiredTestClass());
        if (postgresql != null) {
            postgresql.close();
        }
    }

    static Optional<EmbeddedPostgresqlTest> findAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, EmbeddedPostgresqlTest.class);
    }

    static EmbeddedPostgresql getOrStart(Class<?> testClass, EmbeddedPostgresqlTest annotation) {
        return INSTANCES.computeIfAbsent(testClass, ignored -> EmbeddedPostgresql.start(annotation));
    }

    record EmbeddedPostgresql(EmbeddedPostgres delegate, String jdbcUrl) {

        private static EmbeddedPostgresql start(EmbeddedPostgresqlTest annotation) {
            EmbeddedPostgres postgres = null;
            try {
                postgres = EmbeddedPostgres.start();
                initializeDatabase(postgres, annotation);
                return new EmbeddedPostgresql(
                        postgres,
                        postgres.getJdbcUrl(annotation.username(), annotation.database()));
            } catch (Exception ex) {
                if (postgres != null) {
                    try {
                        postgres.close();
                    } catch (Exception closeException) {
                        ex.addSuppressed(closeException);
                    }
                }
                throw new IllegalStateException("Unable to start embedded PostgreSQL", ex);
            }
        }

        private static void initializeDatabase(
                EmbeddedPostgres postgres,
                EmbeddedPostgresqlTest annotation) throws SQLException {
            var adminUrl = postgres.getJdbcUrl("postgres", "postgres");
            try (var connection = DriverManager.getConnection(adminUrl, "postgres", "")) {
                ensureRole(connection, annotation.username(), annotation.password());
                ensureDatabase(connection, annotation.database(), annotation.username());
            }
        }

        private static void ensureRole(
                java.sql.Connection connection,
                String username,
                String password) throws SQLException {
            var roleExists = false;
            try (var query = connection.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
                query.setString(1, username);
                try (var result = query.executeQuery()) {
                    roleExists = result.next();
                }
            }

            try (var statement = connection.createStatement()) {
                if (!roleExists) {
                    var passwordClause = password.isEmpty()
                            ? ""
                            : " PASSWORD '" + quoteLiteral(password) + "'";
                    statement.execute("CREATE ROLE " + quoteIdentifier(username) + " LOGIN" + passwordClause);
                } else if (!password.isEmpty()) {
                    statement.execute("ALTER ROLE " + quoteIdentifier(username) +
                            " PASSWORD '" + quoteLiteral(password) + "'");
                }
            }
        }

        private static void ensureDatabase(
                java.sql.Connection connection,
                String database,
                String owner) throws SQLException {
            String currentOwner = null;
            try (var query = connection.prepareStatement(
                    "SELECT pg_get_userbyid(datdba) AS owner_name FROM pg_database WHERE datname = ?")) {
                query.setString(1, database);
                try (var result = query.executeQuery()) {
                    if (result.next()) {
                        currentOwner = result.getString("owner_name");
                    }
                }
            }

            try (var statement = connection.createStatement()) {
                if (currentOwner == null) {
                    statement.execute("CREATE DATABASE " + quoteIdentifier(database) +
                            " OWNER " + quoteIdentifier(owner));
                } else if (!currentOwner.equals(owner)) {
                    statement.execute("ALTER DATABASE " + quoteIdentifier(database) +
                            " OWNER TO " + quoteIdentifier(owner));
                }
            }
        }

        private static String quoteIdentifier(String identifier) {
            if (identifier == null || identifier.isBlank()) {
                throw new IllegalArgumentException("PostgreSQL identifier must not be blank");
            }
            return '"' + identifier.replace("\"", "\"\"") + '"';
        }

        private static String quoteLiteral(String value) {
            return value.replace("'", "''");
        }

        private void close() {
            try {
                delegate.close();
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to stop embedded PostgreSQL", ex);
            }
        }
    }
}
