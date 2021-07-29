package com.rbkmoney.testcontainers.annotations.postgresql;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.startContainer;

@Slf4j
public class PostgresqlTestcontainerExtension
        implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<PostgreSQLContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        var annotation = findPostgresqlTestcontainerSingletonAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = PostgresqlTestcontainerFactory.singletonContainer();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var annotation = findPostgresqlTestcontainerAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = PostgresqlTestcontainerFactory.container();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findPostgresqlTestcontainerAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findPostgresqlTestcontainerSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<PostgresqlTestcontainer> findPostgresqlTestcontainerAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainer> findPostgresqlTestcontainerAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findPostgresqlTestcontainerSingletonAnnotation(
            ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), PostgresqlTestcontainerSingleton.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findPostgresqlTestcontainerSingletonAnnotation(
            Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainerSingleton.class);
    }

    public static class PostgresqlTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findPostgresqlTestcontainerAnnotation(testClass).isPresent()) {
                    init(context, findPostgresqlTestcontainerAnnotation(testClass).get().properties());
                } else if (findPostgresqlTestcontainerSingletonAnnotation(testClass).isPresent()) {
                    init(context, findPostgresqlTestcontainerSingletonAnnotation(testClass).get().properties());
                }
            };
        }

        private void init(ConfigurableApplicationContext context, String[] properties) {
            var container = THREAD_CONTAINER.get();
            var jdbcUrl = container.getJdbcUrl();
            var username = container.getUsername();
            var password = container.getPassword();
            TestPropertyValues.of(
                    "spring.datasource.url=" + jdbcUrl,
                    "spring.datasource.username=" + username,
                    "spring.datasource.password=" + password,
                    "spring.flyway.url=" + jdbcUrl,
                    "spring.flyway.user=" + username,
                    "spring.flyway.password=" + password,
                    "flyway.url=" + jdbcUrl,
                    "flyway.user=" + username,
                    "flyway.password=" + password)
                    .and(properties)
                    .applyTo(context);
        }
    }
}
