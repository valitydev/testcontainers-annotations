package com.rbkmoney.testcontainers.annotations.clickhouse;

import com.rbkmoney.clickhouse.initializer.ChInitializer;
import com.rbkmoney.testcontainers.annotations.exception.ClickhouseStartingException;
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
import org.testcontainers.containers.ClickHouseContainer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.startContainer;

@Slf4j
public class ClickhouseTestcontainerExtension
        implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<ClickHouseContainer> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        var annotation = findClickhouseTestcontainerSingletonAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = ClickhouseTestcontainerFactory.singletonContainer();
        if (!container.isRunning()) {
            startContainer(container);
            appliedMigrations(container, annotation.get().migrations());
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var annotation = findClickhouseTestcontainerAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = ClickhouseTestcontainerFactory.container();
        if (!container.isRunning()) {
            startContainer(container);
            appliedMigrations(container, annotation.get().migrations());
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findClickhouseTestcontainerAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findClickhouseTestcontainerSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<ClickhouseTestcontainer> findClickhouseTestcontainerAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainer> findClickhouseTestcontainerAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findClickhouseTestcontainerSingletonAnnotation(
            ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), ClickhouseTestcontainerSingleton.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findClickhouseTestcontainerSingletonAnnotation(
            Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainerSingleton.class);
    }

    private void appliedMigrations(ClickHouseContainer container, String[] migrations) {
        try {
            ChInitializer.initAllScripts(container, Arrays.asList(migrations));
            log.info("Successfully applied " + migrations.length + " migrations");
        } catch (SQLException ex) {
            throw new ClickhouseStartingException(
                    "Error then applied " + migrations.length + " migrations, ",
                    ex);
        }
    }

    public static class ClickhouseTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findClickhouseTestcontainerAnnotation(testClass).isPresent()) {
                    init(context, findClickhouseTestcontainerAnnotation(testClass).get().properties());
                } else if (findClickhouseTestcontainerSingletonAnnotation(testClass).isPresent()) {
                    init(context, findClickhouseTestcontainerSingletonAnnotation(testClass).get().properties());
                }
            };
        }

        private void init(ConfigurableApplicationContext context, String[] properties) {
            var container = THREAD_CONTAINER.get();
            TestPropertyValues.of(
                    "clickhouse.db.url=" + container.getJdbcUrl(),
                    "clickhouse.db.user=" + container.getUsername(),
                    "clickhouse.db.password=" + container.getPassword())
                    .and(properties)
                    .applyTo(context);
        }
    }
}
