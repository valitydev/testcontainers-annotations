package dev.vality.testcontainers.annotations.clickhouse;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SharedTestResourceLock;
import dev.vality.testcontainers.annotations.util.TestExecutionLock;
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
import org.springframework.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClickhouseTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ConcurrentMap<Class<?>, ContainerReference> CONTAINERS = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        getOrStart(context.getRequiredTestClass());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        TestExecutionLock.acquire(context);
        try {
            var reference = getOrStart(context.getRequiredTestClass());
            reference.container().dropDatabase();
            reference.container().applyMigrations();
        } catch (RuntimeException ex) {
            TestExecutionLock.release(context);
            throw ex;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var reference = CONTAINERS.remove(testClass);
        try {
            if (reference != null && !reference.singleton()) {
                reference.container().stop();
            }
        } finally {
            if (reference != null && reference.singleton()) {
                SharedTestResourceLock.release(testClass);
            }
        }
    }

    private static ContainerReference getOrStart(Class<?> testClass) {
        return CONTAINERS.computeIfAbsent(testClass, ClickhouseTestcontainerExtension::createAndStart);
    }

    private static ContainerReference createAndStart(Class<?> testClass) {
        var prototype = findPrototypeAnnotation(testClass);
        var singletonAnnotation = findSingletonAnnotation(testClass);
        if (prototype.isEmpty() && singletonAnnotation.isEmpty()) {
            throw new IllegalStateException("ClickHouse test annotation not found");
        }

        var singleton = singletonAnnotation.isPresent();
        if (singleton) {
            SharedTestResourceLock.acquire(testClass);
        }
        try {
            var container = prototype
                    .map(annotation -> ClickhouseTestcontainerFactory.container(
                            annotation.dbNameShouldBeDropped(),
                            annotation.migrations()))
                    .orElseGet(() -> {
                        var annotation = singletonAnnotation.orElseThrow();
                        return ClickhouseTestcontainerFactory.singletonContainer(
                                annotation.dbNameShouldBeDropped(),
                                annotation.migrations());
                    });
            try {
                GenericContainerUtil.startContainer(container);
                container.dropDatabase();
                container.applyMigrations();
                return new ContainerReference(container, singleton);
            } catch (RuntimeException ex) {
                try {
                    if (singleton) {
                        ClickhouseTestcontainerFactory.discardSingleton(container);
                    } else {
                        container.stop();
                    }
                } catch (RuntimeException stopException) {
                    ex.addSuppressed(stopException);
                }
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (singleton) {
                SharedTestResourceLock.release(testClass);
            }
            throw ex;
        }
    }

    private static Optional<ClickhouseTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainerSingleton.class);
    }

    private static final class ContainerReference {

        private final ClickhouseContainerExtension container;
        private final boolean singleton;

        private ContainerReference(ClickhouseContainerExtension container, boolean singleton) {
            this.container = container;
            this.singleton = singleton;
        }

        private ClickhouseContainerExtension container() {
            return container;
        }

        private boolean singleton() {
            return singleton;
        }

    }

    public static class ClickhouseTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            var prototype = findPrototypeAnnotation(testClass);
            if (prototype.isPresent()) {
                var annotation = prototype.get();
                return new ClickhouseContextCustomizer(
                        testClass,
                        false,
                        annotation.dbNameShouldBeDropped(),
                        List.copyOf(Arrays.asList(annotation.migrations())),
                        List.copyOf(Arrays.asList(annotation.properties())));
            }
            var singleton = findSingletonAnnotation(testClass);
            if (singleton.isPresent()) {
                var annotation = singleton.get();
                return new ClickhouseContextCustomizer(
                        testClass,
                        true,
                        annotation.dbNameShouldBeDropped(),
                        List.copyOf(Arrays.asList(annotation.migrations())),
                        List.copyOf(Arrays.asList(annotation.properties())));
            }
            return null;
        }
    }

    private record ClickhouseContextCustomizer(
            Class<?> testClass,
            boolean singleton,
            String databaseName,
            List<String> migrations,
            List<String> properties) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var container = getOrStart(testClass).container();
            TestPropertyValues.of(
                            "clickhouse.db.url=" + container.getJdbcUrl(),
                            "clickhouse.db.user=" + container.getUsername(),
                            "clickhouse.db.username=" + container.getUsername(),
                            "clickhouse.db.password=" + container.getPassword())
                    .and(properties.toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
