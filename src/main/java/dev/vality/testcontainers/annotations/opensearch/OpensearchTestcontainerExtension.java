package dev.vality.testcontainers.annotations.opensearch;

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
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OpensearchTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ConcurrentMap<Class<?>, ContainerReference> CONTAINERS = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        getOrStart(testClass, configuration(testClass));
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var configuration = configuration(testClass);
        if (configuration.cleanupIndexes()) {
            TestExecutionLock.acquire(context);
            try {
                var reference = getOrStart(testClass, configuration);
                OpensearchIndexCleaner.cleanup(
                        reference.container(),
                        configuration.indexPrefixes(),
                        configuration.excludedIndexes());
            } catch (RuntimeException ex) {
                TestExecutionLock.release(context);
                throw ex;
            }
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

    private static ContainerReference getOrStart(Class<?> testClass, Configuration configuration) {
        return CONTAINERS.computeIfAbsent(testClass, ignored -> createAndStart(testClass, configuration));
    }

    private static ContainerReference createAndStart(Class<?> testClass, Configuration configuration) {
        if (configuration.singleton()) {
            SharedTestResourceLock.acquire(testClass);
        }
        try {
            var container = configuration.singleton()
                    ? OpensearchTestcontainerFactory.singletonContainer()
                    : OpensearchTestcontainerFactory.container();
            try {
                GenericContainerUtil.startContainer(container);
                if (configuration.singleton() && configuration.cleanupIndexes()) {
                    OpensearchIndexCleaner.cleanup(
                            container,
                            configuration.indexPrefixes(),
                            configuration.excludedIndexes());
                }
                return new ContainerReference(container, configuration.singleton());
            } catch (RuntimeException ex) {
                try {
                    if (configuration.singleton()) {
                        OpensearchTestcontainerFactory.discardSingleton(container);
                    } else {
                        container.stop();
                    }
                } catch (RuntimeException stopException) {
                    ex.addSuppressed(stopException);
                }
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (configuration.singleton()) {
                SharedTestResourceLock.release(testClass);
            }
            throw ex;
        }
    }

    private static Configuration configuration(Class<?> testClass) {
        var prototype = findPrototypeAnnotation(testClass);
        if (prototype.isPresent()) {
            var annotation = prototype.get();
            return new Configuration(
                    false,
                    annotation.cleanupIndexes(),
                    List.copyOf(Arrays.asList(annotation.indexPrefixes())),
                    List.copyOf(Arrays.asList(annotation.excludeIndexes())),
                    List.copyOf(Arrays.asList(annotation.properties())));
        }
        var annotation = findSingletonAnnotation(testClass)
                .orElseThrow(() -> new IllegalStateException("OpenSearch test annotation not found"));
        return new Configuration(
                true,
                annotation.cleanupIndexes(),
                List.copyOf(Arrays.asList(annotation.indexPrefixes())),
                List.copyOf(Arrays.asList(annotation.excludeIndexes())),
                List.copyOf(Arrays.asList(annotation.properties())));
    }

    private static Optional<OpensearchTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, OpensearchTestcontainer.class);
    }

    private static Optional<OpensearchTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, OpensearchTestcontainerSingleton.class);
    }

    private static final class ContainerReference {

        private final GenericContainer<?> container;
        private final boolean singleton;

        private ContainerReference(GenericContainer<?> container, boolean singleton) {
            this.container = container;
            this.singleton = singleton;
        }

        private GenericContainer<?> container() {
            return container;
        }

        private boolean singleton() {
            return singleton;
        }

    }

    private record Configuration(
            boolean singleton,
            boolean cleanupIndexes,
            List<String> indexPrefixes,
            List<String> excludedIndexes,
            List<String> properties) {
    }

    public static class OpensearchTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            if (findPrototypeAnnotation(testClass).isEmpty() && findSingletonAnnotation(testClass).isEmpty()) {
                return null;
            }
            return new OpensearchContextCustomizer(testClass, configuration(testClass));
        }
    }

    private record OpensearchContextCustomizer(
            Class<?> testClass,
            Configuration configuration) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var container = getOrStart(testClass, configuration).container();
            TestPropertyValues.of(
                            "opensearch.hostname=" + container.getHost(),
                            "opensearch.port=" + container.getFirstMappedPort())
                    .and(configuration.properties().toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
