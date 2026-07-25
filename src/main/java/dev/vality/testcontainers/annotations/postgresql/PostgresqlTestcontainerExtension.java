package dev.vality.testcontainers.annotations.postgresql;

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

public class PostgresqlTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ConcurrentMap<Class<?>, ContainerReference> CONTAINERS = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        getOrStart(context.getRequiredTestClass());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        findCleanupConfiguration(testClass).ifPresent(configuration -> {
            if (configuration.truncateTables()) {
                TestExecutionLock.acquire(context);
                try {
                    var reference = getOrStart(testClass);
                    reference.container().cleanupDatabaseTables(configuration.excludedTables());
                } catch (RuntimeException ex) {
                    TestExecutionLock.release(context);
                    throw ex;
                }
            }
        });
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
        return CONTAINERS.computeIfAbsent(testClass, PostgresqlTestcontainerExtension::createAndStart);
    }

    private static ContainerReference createAndStart(Class<?> testClass) {
        var prototype = findPrototypeAnnotation(testClass);
        var singletonAnnotation = findSingletonAnnotation(testClass);
        if (prototype.isEmpty() && singletonAnnotation.isEmpty()) {
            throw new IllegalStateException("PostgreSQL test annotation not found");
        }

        var singleton = singletonAnnotation.isPresent();
        if (singleton) {
            SharedTestResourceLock.acquire(testClass);
        }
        try {
            var container = singleton
                    ? PostgresqlTestcontainerFactory.singletonContainer()
                    : PostgresqlTestcontainerFactory.container();
            try {
                GenericContainerUtil.startContainer(container);
                if (singleton) {
                    findCleanupConfiguration(testClass).ifPresent(configuration -> {
                        if (configuration.truncateTables()) {
                            container.cleanupDatabaseTables(configuration.excludedTables());
                        }
                    });
                }
                return new ContainerReference(container, singleton);
            } catch (RuntimeException ex) {
                try {
                    if (singleton) {
                        PostgresqlTestcontainerFactory.discardSingleton(container);
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

    private static Optional<CleanupConfiguration> findCleanupConfiguration(Class<?> testClass) {
        var prototype = findPrototypeAnnotation(testClass);
        if (prototype.isPresent()) {
            var annotation = prototype.get();
            return Optional.of(new CleanupConfiguration(
                    annotation.truncateTables(),
                    List.copyOf(Arrays.asList(annotation.excludeTruncateTables()))));
        }
        return findSingletonAnnotation(testClass)
                .map(annotation -> new CleanupConfiguration(
                        annotation.truncateTables(),
                        List.copyOf(Arrays.asList(annotation.excludeTruncateTables()))));
    }

    private static Optional<PostgresqlTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainerSingleton.class);
    }

    private static final class ContainerReference {

        private final PostgresqlContainerExtension container;
        private final boolean singleton;

        private ContainerReference(PostgresqlContainerExtension container, boolean singleton) {
            this.container = container;
            this.singleton = singleton;
        }

        private PostgresqlContainerExtension container() {
            return container;
        }

        private boolean singleton() {
            return singleton;
        }

    }

    private record CleanupConfiguration(boolean truncateTables, List<String> excludedTables) {
    }

    public static class PostgresqlTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            var prototype = findPrototypeAnnotation(testClass);
            if (prototype.isPresent()) {
                return new PostgresqlContextCustomizer(
                        testClass,
                        false,
                        List.copyOf(Arrays.asList(prototype.get().properties())));
            }
            var singleton = findSingletonAnnotation(testClass);
            if (singleton.isPresent()) {
                return new PostgresqlContextCustomizer(
                        testClass,
                        true,
                        List.copyOf(Arrays.asList(singleton.get().properties())));
            }
            return null;
        }
    }

    private record PostgresqlContextCustomizer(
            Class<?> testClass,
            boolean singleton,
            List<String> properties) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var container = getOrStart(testClass).container();
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
