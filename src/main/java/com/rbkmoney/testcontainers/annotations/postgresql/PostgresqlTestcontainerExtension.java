package com.rbkmoney.testcontainers.annotations.postgresql;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
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
public class PostgresqlTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<PostgreSQLContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            init(PostgresqlTestcontainerFactory.container());
        } else if (findSingletonAnnotation(context).isPresent()) {
            init(PostgresqlTestcontainerFactory.singletonContainer());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<PostgresqlTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), PostgresqlTestcontainerSingleton.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainerSingleton.class);
    }

    private void init(PostgreSQLContainer<? extends PostgreSQLContainer<?>> container) {
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    public static class PostgresqlTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findPrototypeAnnotation(testClass).isPresent()) {
                    init(context, findPrototypeAnnotation(testClass).get().properties()); //NOSONAR
                } else if (findSingletonAnnotation(testClass).isPresent()) {
                    init(context, findSingletonAnnotation(testClass).get().properties()); //NOSONAR
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
