package dev.vality.testcontainers.annotations.clickhouse;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.Optional;

/**
 * {@code @ClickhouseTestcontainerExtension} инициализирует тестконтейнер из {@link ClickhouseTestcontainerFactory},
 * настраивает, стартует, валидирует и останавливает
 * <p><h3>{@link ClickhouseTestcontainerExtension.ClickhouseTestcontainerContextCustomizerFactory}</h3>
 * <p>Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован
 * под капотом аннотаций, на уровне реализации интерфейса  —
 * информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки
 * инициализируются через {@link TestPropertyValues} и сливаются с текущим получаемым контекстом
 * приложения {@link ConfigurableApplicationContext}
 * <p>Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов
 * в файле META-INF/spring.factories
 *
 * @see ClickhouseTestcontainerFactory ClickhouseTestcontainerFactory
 * @see ClickhouseTestcontainerExtension.ClickhouseTestcontainerContextCustomizerFactory ClickhouseTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class ClickhouseTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ThreadLocal<ClickhouseContainerExtension> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var annotation = findPrototypeAnnotation(context).get();
            var container = ClickhouseTestcontainerFactory.container(annotation.dbNameShouldBeDropped(),
                    annotation.migrations());
            GenericContainerUtil.startContainer(container);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var annotation = findSingletonAnnotation(context).get();
            var container = ClickhouseTestcontainerFactory.singletonContainer(annotation.dbNameShouldBeDropped(),
                    annotation.migrations());
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
            container.dropDatabase();
            container.appliedMigrations();
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

    private static Optional<ClickhouseTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), ClickhouseTestcontainerSingleton.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainerSingleton.class);
    }

    public static class ClickhouseTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

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
                            "clickhouse.db.url=" + container.getJdbcUrl(),
                            "clickhouse.db.user=" + container.getUsername(),
                            "clickhouse.db.username=" + container.getUsername(),
                            "clickhouse.db.password=" + container.getPassword())
                    .and(properties)
                    .applyTo(context);
        }
    }
}
