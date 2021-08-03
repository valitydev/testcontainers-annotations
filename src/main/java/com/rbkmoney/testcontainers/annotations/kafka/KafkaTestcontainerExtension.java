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
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.KafkaContainer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class KafkaTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<KafkaContainer> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = KafkaTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            List<String> topics = loadTopics(findPrototypeAnnotation(context).get().topicsKeys()); //NOSONAR
            createTopics(container, topics);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var container = KafkaTestcontainerFactory.singletonContainer();
            var topics = loadTopics(findSingletonAnnotation(context).get().topicsKeys()); //NOSONAR
            if (!container.isRunning()) {
                GenericContainerUtil.startContainer(container);
            } else {
                deleteTopics(container, topics);
            }
            createTopics(container, topics);
            THREAD_CONTAINER.set(container);
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

    private static Optional<KafkaTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), KafkaTestcontainerSingleton.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainerSingleton.class);
    }

    private List<String> loadTopics(String[] topicsKeys) {
        return loadFromSpringApplicationPropertiesFile(Arrays.asList(topicsKeys))
                .values().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
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
            Set<String> adminClientTopics = admin.listTopics().names().get(30, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS CREATED]: " + adminClientTopics);
            assertThat(adminClientTopics.size())
                    .isEqualTo(topics.size());
            // make sure Zookeeper is ready before creating a new topic
            assertThat(execInContainerKafkaTopicsListCommand(container))
                    .contains(topics);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic creating, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic creating, ", ex);
        }
    }

    private void deleteTopics(KafkaContainer container, List<String> topics) {
        try (var admin = createAdminClient(container)) {
            var topicsResult = admin.deleteTopics(topics);
            // wait until everyone is deleted or timeout
            topicsResult.all().get(30, TimeUnit.SECONDS);
            Set<String> adminClientTopics = admin.listTopics().names().get(30, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS DELETED]: " +
                    adminClientTopics + " (should be empty)");
            assertThat(adminClientTopics)
                    .isEmpty();
            // make sure all metadata has been deleted successfully from within Zookeeper before creating a new topic
            execInContainerKafkaTopicsListCommand(container);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic deleting, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic deleting, ", ex);
        }
    }

    private AdminClient createAdminClient(KafkaContainer container) {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
        return AdminClient.create(properties);
    }

    private String execInContainerKafkaTopicsListCommand(KafkaContainer container) {
        var kafkaTopicsListCommand = "/usr/bin/kafka-topics --zookeeper localhost:2181 --list";
        try {
            var stdout = container.execInContainer("/bin/sh", "-c", kafkaTopicsListCommand)
                    .getStdout();
            log.info("Topics list from '/usr/bin/kafka-topics': " +
                    "[" + stdout.replace("\n", ",") + "]");
            return stdout;
        } catch (IOException ex) {
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        }
    }

    public static class KafkaTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

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
            TestPropertyValues.of(
                    "kafka.bootstrap-servers=" + container.getBootstrapServers(),
                    "kafka.ssl.enabled=false")
                    .and(properties)
                    .applyTo(context);
        }
    }
}
