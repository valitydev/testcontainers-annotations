package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
 * создании топиков через {@link AdminClient} в {@link KafkaContainerExtension#createTopics(List)},
 * а также валидации результата создания через запрос '/usr/bin/kafka-topics --zookeeper localhost:2181 --list'
 * напрямую в контейнере в {@link KafkaContainerExtension#execInContainerKafkaTopicsListCommand()}
 * <p>Также помимо перечисленного, при работе расширения для создания синглтона перед запуском тестов
 * в каждом файле будет проводится удаление созданных ранее топиков в {@link KafkaContainerExtension#deleteTopics(List)}
 * и дальнейшее пересоздание топиков в {@link KafkaContainerExtension#createTopics(List)},
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
public class KafkaTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ThreadLocal<KafkaContainerExtension> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var annotation = findPrototypeAnnotation(context).get();
            var topics = loadTopics(annotation.topicsKeys());
            var container = KafkaTestcontainerFactory.container(annotation.provider(), topics);
            GenericContainerUtil.startContainer(container);
            container.createTopics();
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var annotation = findSingletonAnnotation(context).get();
            var topics = loadTopics(annotation.topicsKeys());
            var container = KafkaTestcontainerFactory.singletonContainer(annotation.provider(), topics);
            if (!container.isRunning()) {
                GenericContainerUtil.startContainer(container);
            }
            THREAD_CONTAINER.set(container);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var container = THREAD_CONTAINER.get();
        if (container != null && container.isRunning()) {
            container.deleteTopics();
            container.createTopics();
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
        return AnnotationSupport.findAnnotation(context.getTestClass(), KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, KafkaTestcontainer.class);
    }

    private static Optional<KafkaTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), KafkaTestcontainerSingleton.class);
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

    public static class KafkaTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findPrototypeAnnotation(testClass).isPresent()) {
                    init(context, findPrototypeAnnotation(testClass).get().properties());
                } else if (findSingletonAnnotation(testClass).isPresent()) {
                    init(context, findSingletonAnnotation(testClass).get().properties());
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
