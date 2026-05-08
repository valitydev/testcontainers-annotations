package dev.vality.testcontainers.annotations.clickhouse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

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
    private String singletonDatabaseName;
    private String[] singletonMigrations;

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
            validateSingletonConfig(databaseName, migrations);
            return clickHouseContainer;
        }
        clickHouseContainer = create(databaseName, migrations);
        singletonDatabaseName = databaseName;
        singletonMigrations = Arrays.copyOf(migrations, migrations.length);
        return clickHouseContainer;
    }

    private ClickhouseContainerExtension create(String databaseName, String[] migrations) {
        return new ClickhouseContainerExtension(databaseName, migrations);
    }

    private void validateSingletonConfig(String databaseName, String[] migrations) {
        if (!singletonDatabaseName.equals(databaseName) || !Arrays.equals(singletonMigrations, migrations)) {
            throw new IllegalStateException(
                    ("ClickHouse singleton testcontainer was already created with databaseName=%s migrations=%s, "
                            + "but requested databaseName=%s migrations=%s. Use the same singleton ClickHouse "
                            + "configuration across test classes or switch to non-singleton @ClickhouseTestcontainer.")
                            .formatted(
                                    singletonDatabaseName,
                                    Arrays.toString(singletonMigrations),
                                    databaseName,
                                    Arrays.toString(migrations)));
        }
    }

    private static class SingletonHolder {

        private static final ClickhouseTestcontainerFactory INSTANCE = new ClickhouseTestcontainerFactory();

    }
}
