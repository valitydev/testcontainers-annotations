package com.rbkmoney.testcontainers.annotations.clickhouse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadTagFromSpringApplicationPropertiesFile;

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
                        .withTag(loadTagFromSpringApplicationPropertiesFile(TAG_PROPERTY)))) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final ClickhouseTestcontainerFactory INSTANCE = new ClickhouseTestcontainerFactory();

    }
}
