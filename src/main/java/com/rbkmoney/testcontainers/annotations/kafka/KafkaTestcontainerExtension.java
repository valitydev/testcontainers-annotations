package com.rbkmoney.testcontainers.annotations.kafka;

import com.rbkmoney.testcontainers.annotations.exception.KafkaStartingException;
import com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
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
import org.testcontainers.containers.KafkaContainer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class KafkaTestcontainerExtension
        implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<KafkaContainer> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        var annotation = findKafkaTestcontainerSingletonAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = KafkaTestcontainerFactory.singletonContainer();
        if (!container.isRunning()) {
            startContainer(container, annotation.get().topicsKeys());
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var annotation = findKafkaTestcontainerAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = KafkaTestcontainerFactory.container();
        if (!container.isRunning()) {
            startContainer(container, annotation.get().topicsKeys());
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findKafkaTestcontainerAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findKafkaTestcontainerSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<KafkaTestcontainer> findKafkaTestcontainerAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainer> findKafkaTestcontainerAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findKafkaTestcontainerSingletonAnnotation(
            ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), KafkaTestcontainerSingleton.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findKafkaTestcontainerSingletonAnnotation(
            Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainerSingleton.class);
    }

    private void startContainer(KafkaContainer container, String[] topicsKeys) {
        GenericContainerUtil.startContainer(container);
        var topics = loadFromSpringApplicationPropertiesFile(Arrays.asList(topicsKeys))
                .values().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        createTopics(container, topics);
        parseAndCheckCreatedTopicsFromKafkaContainer(container, topics);
    }

    private void createTopics(KafkaContainer container, List<String> topics) {
        try (var admin = createAdminClient(container)) {
            var newTopics = topics.stream()
                    .map(topic -> new NewTopic(topic, 1, (short) 1))
                    .peek(newTopic -> log.info(newTopic.toString()))
                    .collect(Collectors.toList());
            var topicsResult = admin.createTopics(newTopics);
            // wait until everyone is created or timeout
            topicsResult.all().get(30, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic creating, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic creating, ", ex);
        }
    }

    private AdminClient createAdminClient(KafkaContainer container) {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        return AdminClient.create(properties);
    }

    private void parseAndCheckCreatedTopicsFromKafkaContainer(KafkaContainer container, List<String> topics) {
        var showCreatedTopics = "/usr/bin/kafka-topics --bootstrap-server=0.0.0.0:9092 --list";
        try {
            var stdout = container.execInContainer("/bin/sh", "-c", showCreatedTopics)
                    .getStdout();
            assertThat(stdout)
                    .contains(topics);
        } catch (IOException ex) {
            throw new KafkaStartingException("Error when " + showCreatedTopics + ", ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic creating, ", ex);
        }
    }

    public static class KafkaTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findKafkaTestcontainerAnnotation(testClass).isPresent()) {
                    init(context, findKafkaTestcontainerAnnotation(testClass).get().properties());
                } else if (findKafkaTestcontainerSingletonAnnotation(testClass).isPresent()) {
                    init(context, findKafkaTestcontainerSingletonAnnotation(testClass).get().properties());
                }
            };
        }

        private void init(ConfigurableApplicationContext context, String[] properties) {
            var container = THREAD_CONTAINER.get();
            TestPropertyValues.of(
                    "kafka.bootstrap-servers=" + container.getBootstrapServers(),
                    "kafka.ssl.enabled=false")
                    .and(properties)
                    .applyTo(context);
        }
    }
}
