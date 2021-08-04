package com.rbkmoney.testcontainers.annotations.clickhouse;

import com.rbkmoney.clickhouse.initializer.ChInitializer;
import com.rbkmoney.clickhouse.initializer.ConnectionManager;
import com.rbkmoney.testcontainers.annotations.exception.ClickhouseStartingException;
import com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.ClickHouseContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
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
 * <p><h3>Нюансы</h3>
 * <p>Дополнительно данное расширение накатывает
 * файлы с миграциями {@link #appliedMigrations(ClickHouseContainer, String[])}
 * при запуске тестконтейнера
 * <p>Также помимо перечисленного, при работе расширения для создания синглтона перед запуском тестов
 * в каждом файле будет проводится удаление базы данных
 * в {@link #dropDatabase(ClickhouseTestcontainerSingleton, ClickHouseContainer)},
 * которая ранее была указана в
 * {@link ClickhouseTestcontainerSingleton#dbNameShouldBeDropped()},
 * таким образом обеспечивая изоляцию данных между файлами с тестами
 * <p>Для работы с миграциями используется авторская библиотека Константина Стружкина
 * com.rbkmoney:clickhouse-test
 *
 * @see ClickhouseTestcontainerFactory ClickhouseTestcontainerFactory
 * @see ClickhouseTestcontainerExtension.ClickhouseTestcontainerContextCustomizerFactory ClickhouseTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class ClickhouseTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<ClickHouseContainer> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = ClickhouseTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            appliedMigrations(container, findPrototypeAnnotation(context).get().migrations()); //NOSONAR
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var annotation = findSingletonAnnotation(context).get(); //NOSONAR
            var container = ClickhouseTestcontainerFactory.singletonContainer();
            if (!container.isRunning()) {
                GenericContainerUtil.startContainer(container);
            } else {
                dropDatabase(annotation, container);
            }
            appliedMigrations(container, annotation.migrations());
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

    private static Optional<ClickhouseTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainer.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), ClickhouseTestcontainerSingleton.class);
    }

    private static Optional<ClickhouseTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, ClickhouseTestcontainerSingleton.class);
    }

    private void appliedMigrations(ClickHouseContainer container, String[] migrations) {
        try {
            ChInitializer.initAllScripts(container, Arrays.asList(migrations));
            log.info("Successfully applied " + migrations.length + " migrations");
        } catch (SQLException ex) {
            throw new ClickhouseStartingException(
                    "Error then applied " + migrations.length + " migrations, ",
                    ex);
        }
    }

    private void dropDatabase(ClickhouseTestcontainerSingleton annotation, ClickHouseContainer container) {
        try (Connection connection = ConnectionManager.getSystemConn(container)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(String.format("DROP DATABASE IF EXISTS %s", annotation.dbNameShouldBeDropped()));
            }
            log.info(String.format("Successfully DROP DATABASE IF EXISTS %s", annotation.dbNameShouldBeDropped()));
        } catch (SQLException ex) {
            throw new ClickhouseStartingException(
                    "Error then drop database dbName=" + annotation.dbNameShouldBeDropped() + ", ",
                    ex);
        }
    }

    public static class ClickhouseTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

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
                    "clickhouse.db.url=" + container.getJdbcUrl(),
                    "clickhouse.db.user=" + container.getUsername(),
                    "clickhouse.db.password=" + container.getPassword())
                    .and(properties)
                    .applyTo(context);
        }
    }
}
