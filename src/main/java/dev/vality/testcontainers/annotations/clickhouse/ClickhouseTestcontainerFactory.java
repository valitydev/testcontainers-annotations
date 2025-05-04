package dev.vality.testcontainers.annotations.clickhouse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create(String, String[])} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer(String, String[])} создает синглтон тестконтейнера
 *
 * @see ClickhouseTestcontainerExtension ClickhouseTestcontainerExtension
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClickhouseTestcontainerFactory {

    private ClickhouseContainerExtension clickHouseContainer;

    public static ClickhouseContainerExtension container(String databaseName, String[] migrations) {
        return instance().create(databaseName, migrations);
    }

    public static ClickhouseContainerExtension singletonContainer(String databaseName, String[] migrations) {
        return instance().getOrCreateSingletonContainer(databaseName, migrations);
    }

    private static ClickhouseTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private ClickhouseContainerExtension getOrCreateSingletonContainer(String databaseName, String[] migrations) {
        if (clickHouseContainer != null) {
            return clickHouseContainer;
        }
        clickHouseContainer = create(databaseName, migrations);
        return clickHouseContainer;
    }

    private ClickhouseContainerExtension create(String databaseName, String[] migrations) {
        try (var container = new ClickhouseContainerExtension(databaseName, migrations)) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final ClickhouseTestcontainerFactory INSTANCE = new ClickhouseTestcontainerFactory();

    }
}
