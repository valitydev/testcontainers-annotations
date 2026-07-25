package dev.vality.testcontainers.annotations.postgresql;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.List;

public class EmbeddedPostgresqlTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return EmbeddedPostgresqlTestExtension.findAnnotation(testClass)
                .<ContextCustomizer>map(annotation -> new EmbeddedPostgresqlContextCustomizer(
                        testClass,
                        annotation.database(),
                        annotation.username(),
                        annotation.password(),
                        List.copyOf(Arrays.asList(annotation.properties()))))
                .orElse(null);
    }

    private record EmbeddedPostgresqlContextCustomizer(
            Class<?> testClass,
            String database,
            String username,
            String password,
            List<String> properties) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var annotation = EmbeddedPostgresqlTestExtension.findAnnotation(testClass)
                    .orElseThrow(() -> new IllegalStateException("Embedded PostgreSQL annotation not found"));
            var postgresql = EmbeddedPostgresqlTestExtension.getOrStart(testClass, annotation);
            var jdbcUrl = postgresql.jdbcUrl();
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
                    .and(properties.toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
