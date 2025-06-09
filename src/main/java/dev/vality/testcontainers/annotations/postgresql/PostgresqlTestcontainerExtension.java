package dev.vality.testcontainers.annotations.postgresql;

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
 * {@code @PostgresqlTestcontainerExtension} инициализирует тестконтейнер из {@link PostgresqlTestcontainerFactory},
 * настраивает, стартует, валидирует и останавливает
 * <p><h3>{@link PostgresqlTestcontainerContextCustomizerFactory}</h3>
 * <p>Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован
 * под капотом аннотаций, на уровне реализации интерфейса  —
 * информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки
 * инициализируются через {@link TestPropertyValues} и сливаются с текущим получаемым контекстом
 * приложения {@link ConfigurableApplicationContext}
 * <p>Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов
 * в файле META-INF/spring.factories
 *
 * @see PostgresqlTestcontainerFactory PostgresqlTestcontainerFactory
 * @see PostgresqlTestcontainerContextCustomizerFactory PostgresqlTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class PostgresqlTestcontainerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ThreadLocal<PostgresqlContainerExtension> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = PostgresqlTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var container = PostgresqlTestcontainerFactory.singletonContainer();
            if (!container.isRunning()) {
                GenericContainerUtil.startContainer(container);
            }
            THREAD_CONTAINER.set(container);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var container = THREAD_CONTAINER.get();
        var annotation = findSingletonAnnotation(context);
        var truncateTablesFlag = annotation.isEmpty() || annotation.get().truncateTables();
        if (container != null && container.isRunning() && truncateTablesFlag) {
            container.cleanupDatabaseTables();
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

    private static Optional<PostgresqlTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainer.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), PostgresqlTestcontainerSingleton.class);
    }

    private static Optional<PostgresqlTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, PostgresqlTestcontainerSingleton.class);
    }

    public static class PostgresqlTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

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
                    .and(properties)
                    .applyTo(context);
        }
    }
}
