package dev.vality.testcontainers.annotations.postgresql;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;

public class EmbeddedPostgresqlTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) ->
                EmbeddedPostgresqlTestExtension.findAnnotation(testClass)
                        .ifPresent(annotation -> init(context, annotation));
    }

    private void init(ConfigurableApplicationContext context, EmbeddedPostgresqlTest annotation) {
        var postgresql = EmbeddedPostgresqlTestExtension.getOrStart(annotation);
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
