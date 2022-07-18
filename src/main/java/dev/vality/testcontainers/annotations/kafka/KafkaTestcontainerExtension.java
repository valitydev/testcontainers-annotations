package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.exception.KafkaStartingException;
import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @KafkaTestcontainerExtension} инициализирует тестконтейнер из {@link KafkaTestcontainerFactory},
 * настраивает, стартует, валидирует и останавливает
 * <p><h3>{@link KafkaTestcontainerExtension.KafkaTestcontainerContextCustomizerFactory}</h3>
 * <p>Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован
 * под капотом аннотаций, на уровне реализации интерфейса  —
 * информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки
 * инициализируются через {@link TestPropertyValues} и сливаются с текущим получаемым контекстом
 * приложения {@link ConfigurableApplicationContext}
 * <p>Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов
 * в файле META-INF/spring.factories
 * <p><h3>Нюансы</h3>
 * <p>Данное расширение немного сложнее других аналогичных в библиотеке за счет дополнительной работы с топиками
 * <p>Работа заключается в загрузке имен топиков из файла с настройками спринга {@link #loadTopics(String[])},
 * создании топиков через {@link AdminClient} в {@link #createTopics(KafkaContainer, List)},
 * а также валидации результата создания через запрос '/usr/bin/kafka-topics --zookeeper localhost:2181 --list'
 * напрямую в контейнере в {@link #execInContainerKafkaTopicsListCommand(KafkaContainer)}
 * <p>Также помимо перечисленного, при работе расширения для создания синглтона перед запуском тестов
 * в каждом файле будет проводится удаление созданных ранее топиков в {@link #deleteTopics(KafkaContainer, List)}
 * и дальнейшее пересоздание топиков в {@link #createTopics(KafkaContainer, List)},
 * таким образом обеспечивая изоляцию данных между файлами с тестами
 *
 * @see KafkaTestcontainerFactory KafkaTestcontainerFactory
 * @see KafkaTestcontainerExtension.KafkaTestcontainerContextCustomizerFactory KafkaTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class KafkaTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<KafkaContainer> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = KafkaTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            var topics = loadTopics(findPrototypeAnnotation(context).get().topicsKeys()); //NOSONAR
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
        return SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile(Arrays.asList(topicsKeys))
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
            var adminClientTopics = admin.listTopics().names().get(30, TimeUnit.SECONDS);
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
            var adminClientTopics = admin.listTopics().names().get(30, TimeUnit.SECONDS);
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
        var kafkaTopicsListCommand = "/usr/bin/kafka-topics --bootstrap-server localhost:9092 --list";
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
                            "spring.kafka.bootstrap-servers=" + container.getBootstrapServers(),
                            "kafka.ssl.enabled=false")
                    .and(properties)
                    .applyTo(context);
        }
    }
}
