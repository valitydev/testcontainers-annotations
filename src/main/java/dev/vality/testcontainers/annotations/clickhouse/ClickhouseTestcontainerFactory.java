package dev.vality.testcontainers.annotations.clickhouse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 *
 * @see ClickhouseTestcontainerExtension ClickhouseTestcontainerExtension
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClickhouseTestcontainerFactory {

    private static final String CLICKHOUSE_IMAGE_NAME = "yandex/clickhouse-server";
    private static final String TAG_PROPERTY = "testcontainers.clickhouse.tag";

    private ClickHouseContainer clickHouseContainer;

    public static ClickHouseContainer container() {
        return instance().create();
    }

    public static ClickHouseContainer singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static ClickhouseTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private ClickHouseContainer getOrCreateSingletonContainer() {
        if (clickHouseContainer != null) {
            return clickHouseContainer;
        }
        clickHouseContainer = create();
        return clickHouseContainer;
    }

    private ClickHouseContainer create() {
        try (ClickHouseContainer container = new ClickHouseContainer(
                DockerImageName
                        .parse(CLICKHOUSE_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))) {
            container.withNetworkAliases("clickhouse-server-" + UUID.randomUUID());
            return container;
        }
    }

    private static class SingletonHolder {

        private static final ClickhouseTestcontainerFactory INSTANCE = new ClickhouseTestcontainerFactory();

    }
}
