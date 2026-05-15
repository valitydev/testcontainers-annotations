package dev.vality.testcontainers.annotations.postgresql;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.List;
import java.util.Optional;

public class EmbeddedPostgresqlTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final ThreadLocal<EmbeddedPostgresql> THREAD_POSTGRESQL = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        findAnnotation(context).ifPresent(annotation -> THREAD_POSTGRESQL.set(getOrStart(annotation)));
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        findAnnotation(context).ifPresent(annotation -> {
            if (annotation.truncateTables()) {
                var postgresql = getOrStart(annotation);
                PostgresqlDatabaseCleaner.cleanupDatabaseTables(
                        postgresql.jdbcUrl(),
                        annotation.username(),
                        annotation.password(),
                        List.of(annotation.excludeTruncateTables()));
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        findAnnotation(context).ifPresent(annotation -> {
            var postgresql = THREAD_POSTGRESQL.get();
            THREAD_POSTGRESQL.remove();
            if (postgresql != null) {
                postgresql.close();
            }
        });
    }

    private static Optional<EmbeddedPostgresqlTest> findAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), EmbeddedPostgresqlTest.class);
    }

    static Optional<EmbeddedPostgresqlTest> findAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, EmbeddedPostgresqlTest.class);
    }

    static EmbeddedPostgresql getOrStart(EmbeddedPostgresqlTest annotation) {
        var postgresql = THREAD_POSTGRESQL.get();
        if (postgresql == null) {
            postgresql = EmbeddedPostgresql.start(annotation);
            THREAD_POSTGRESQL.set(postgresql);
        }
        return postgresql;
    }

    record EmbeddedPostgresql(EmbeddedPostgres delegate, String jdbcUrl) {

        @SneakyThrows
        private static EmbeddedPostgresql start(EmbeddedPostgresqlTest annotation) {
            var postgres = EmbeddedPostgres.start();
            return new EmbeddedPostgresql(
                    postgres,
                    postgres.getJdbcUrl(annotation.database(), annotation.username()));
        }

        @SneakyThrows
        private void close() {
            delegate.close();
        }
    }
}
