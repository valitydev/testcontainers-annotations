package com.rbkmoney.testcontainers.annotations.minio;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.getWaitStrategy;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioTestcontainerFactory {

    public static final String MINIO_USER = "testcontainers.minio.user";
    public static final String MINIO_PASSWORD = "testcontainers.minio.password";
    private static final String MINIO_IMAGE_NAME = "minio/minio";
    private static final String TAG_PROPERTY = "testcontainers.minio.tag";

    private GenericContainer<?> minioContainer;

    public static GenericContainer<?> container() {
        return instance().create();
    }

    public static GenericContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static MinioTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private GenericContainer<?> getOrCreateSingletonContainer() {
        if (minioContainer != null) {
            return minioContainer;
        }
        minioContainer = create();
        return minioContainer;
    }

    private GenericContainer<?> create() {
        try (GenericContainer<?> container = new GenericContainer<>(
                DockerImageName
                        .parse(MINIO_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withNetworkAliases("minio")
                .withEnv("MINIO_ROOT_USER", loadDefaultLibraryProperty(MINIO_USER))
                .withEnv("MINIO_ROOT_PASSWORD", loadDefaultLibraryProperty(MINIO_PASSWORD))
                .waitingFor(getWaitStrategy("/minio/health/live", 200, 9000, Duration.ofMinutes(1)))) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final MinioTestcontainerFactory INSTANCE = new MinioTestcontainerFactory();

    }
}
