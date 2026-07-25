package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.kafka.constants.Provider;
import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SharedTestResourceLock;
import dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader;
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

public class KafkaTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

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
        if (configuration.truncateTopics()) {
            TestExecutionLock.acquire(context);
            try {
                var reference = getOrStart(testClass, configuration);
                reference.container().deleteTopics(configuration.excludedTopics());
                reference.container().createTopics(List.of());
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
                    ? KafkaTestcontainerFactory.singletonContainer(configuration.provider(), configuration.topics())
                    : KafkaTestcontainerFactory.container(configuration.provider(), configuration.topics());
            try {
                GenericContainerUtil.startContainer(container);
                if (configuration.singleton() && configuration.truncateTopics()) {
                    container.deleteTopics(configuration.excludedTopics());
                }
                container.createTopics(List.of());
                return new ContainerReference(container, configuration.singleton());
            } catch (RuntimeException ex) {
                try {
                    if (configuration.singleton()) {
                        KafkaTestcontainerFactory.discardSingleton(container);
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
                    annotation.provider(),
                    loadTopics(annotation.topicsKeys()),
                    annotation.truncateTopics(),
                    List.copyOf(Arrays.asList(annotation.excludeTruncateTopics())),
                    List.copyOf(Arrays.asList(annotation.properties())));
        }
        var annotation = findSingletonAnnotation(testClass)
                .orElseThrow(() -> new IllegalStateException("Kafka test annotation not found"));
        return new Configuration(
                true,
                annotation.provider(),
                loadTopics(annotation.topicsKeys()),
                annotation.truncateTopics(),
                List.copyOf(Arrays.asList(annotation.excludeTruncateTopics())),
                List.copyOf(Arrays.asList(annotation.properties())));
    }

    private static Optional<KafkaTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainerSingleton.class);
    }

    private static List<String> loadTopics(String[] topicKeys) {
        var properties = SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile(
                Arrays.asList(topicKeys));
        return Arrays.stream(topicKeys)
                .map(properties::getProperty)
                .map(String::trim)
                .filter(topic -> !topic.isEmpty())
                .distinct()
                .toList();
    }

    private static final class ContainerReference {

        private final KafkaContainerExtension container;
        private final boolean singleton;

        private ContainerReference(KafkaContainerExtension container, boolean singleton) {
            this.container = container;
            this.singleton = singleton;
        }

        private KafkaContainerExtension container() {
            return container;
        }

        private boolean singleton() {
            return singleton;
        }

    }

    private record Configuration(
            boolean singleton,
            Provider provider,
            List<String> topics,
            boolean truncateTopics,
            List<String> excludedTopics,
            List<String> properties) {
    }

    public static class KafkaTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            if (findPrototypeAnnotation(testClass).isEmpty() && findSingletonAnnotation(testClass).isEmpty()) {
                return null;
            }
            return new KafkaContextCustomizer(testClass, configuration(testClass));
        }
    }

    private record KafkaContextCustomizer(
            Class<?> testClass,
            Configuration configuration) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var container = getOrStart(testClass, configuration).container();
            TestPropertyValues.of(
                            "kafka.bootstrap-servers=" + container.getBootstrapServers(),
                            "spring.kafka.bootstrap-servers=" + container.getBootstrapServers(),
                            "kafka.ssl.enabled=false")
                    .and(configuration.properties().toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
