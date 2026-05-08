package dev.vality.testcontainers.annotations.postgresql;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

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

    private static Optional<EmbeddedPostgresqlTest> findAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, EmbeddedPostgresqlTest.class);
    }

    private static EmbeddedPostgresql getOrStart(EmbeddedPostgresqlTest annotation) {
        var postgresql = THREAD_POSTGRESQL.get();
        if (postgresql == null) {
            postgresql = EmbeddedPostgresql.start(annotation);
            THREAD_POSTGRESQL.set(postgresql);
        }
        return postgresql;
    }

    public static class EmbeddedPostgresqlTestContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) ->
                    findAnnotation(testClass).ifPresent(annotation -> init(context, annotation));
        }

        private void init(ConfigurableApplicationContext context, EmbeddedPostgresqlTest annotation) {
            var postgresql = getOrStart(annotation);
            THREAD_POSTGRESQL.set(postgresql);
            var jdbcUrl = postgresql.jdbcUrl();
            var username = annotation.username();
            var password = annotation.password();
            TestPropertyValues.of(
                            "spring.datasource.url=" + jdbcUrl,
                            "spring.datasource.username=" + username,
                            "spring.datasource.password=" + password,
                            "spring.flyway.url=" + jdbcUrl,
                            "spring.flyway.user=" + username,
                            "spring.flyway.password=" + password,
                            "postgres.db.url=" + jdbcUrl,
                            "postgres.db.user=" + username,
                            "postgres.db.username=" + username,
                            "postgres.db.password=" + password,
                            "flyway.url=" + jdbcUrl,
                            "flyway.user=" + username,
                            "flyway.password=" + password,
                            "flyway.postgresql.transactional.lock=false")
                    .and(annotation.properties())
                    .applyTo(context);
        }
    }

    private record EmbeddedPostgresql(EmbeddedPostgres delegate, String jdbcUrl) {

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
