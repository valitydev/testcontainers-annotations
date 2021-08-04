package com.rbkmoney.testcontainers.annotations.minio;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.getWaitStrategy;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 * <p>{@link #MINIO_USER} необходимо указать в файле application.yml при необходимости другого ключа
 * <p>{@link #MINIO_PASSWORD} необходимо указать в файле application.yml при необходимости другого ключа
 *
 * @see MinioTestcontainerExtension MinioTestcontainerExtension
 */
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
                .withExposedPorts(9000)
                .withNetworkAliases("minio-" + UUID.randomUUID())
                .withEnv("MINIO_ROOT_USER", loadDefaultLibraryProperty(MINIO_USER))
                .withEnv("MINIO_ROOT_PASSWORD", loadDefaultLibraryProperty(MINIO_PASSWORD))
                .withCommand("server /data")
                .waitingFor(getWaitStrategy("/minio/health/live", 200, 9000, Duration.ofMinutes(1)))) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final MinioTestcontainerFactory INSTANCE = new MinioTestcontainerFactory();

    }
}
