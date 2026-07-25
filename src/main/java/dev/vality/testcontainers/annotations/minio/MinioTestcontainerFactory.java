package dev.vality.testcontainers.annotations.minio;

import dev.vality.testcontainers.annotations.util.ContainerShutdownRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static dev.vality.testcontainers.annotations.util.GenericContainerUtil.getWaitStrategy;
import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * Фабрика по созданию MinIO контейнеров.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MinioTestcontainerFactory {

    public static final String MINIO_USER = "testcontainers.minio.user";
    public static final String MINIO_PASSWORD = "testcontainers.minio.password";
    private static final String MINIO_IMAGE_NAME = "quay.io/minio/minio";
    private static final String TAG_PROPERTY = "testcontainers.minio.tag";

    private volatile GenericContainer<?> singletonContainer;

    public static GenericContainer<?> container() {
        return instance().create();
    }

    public static GenericContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    static void discardSingleton(GenericContainer<?> container) {
        instance().discard(container);
    }

    private static MinioTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    private void discard(GenericContainer<?> container) {
        synchronized (this) {
            if (singletonContainer != container) {
                return;
            }
            singletonContainer = null;
        }
        ContainerShutdownRegistry.unregister(container);
        container.stop();
    }

    private GenericContainer<?> getOrCreateSingletonContainer() {
        var result = singletonContainer;
        if (result == null) {
            synchronized (this) {
                result = singletonContainer;
                if (result == null) {
                    result = ContainerShutdownRegistry.register(create());
                    singletonContainer = result;
                }
            }
        }
        return result;
    }

    private GenericContainer<?> create() {
        return new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", loadDefaultLibraryProperty(MINIO_USER))
                .withEnv("MINIO_ROOT_PASSWORD", loadDefaultLibraryProperty(MINIO_PASSWORD))
                .withCommand("server /data")
                .waitingFor(getWaitStrategy("/minio/health/live", 200, 9000, Duration.ofMinutes(1)));
    }

    private static class SingletonHolder {

        private static final MinioTestcontainerFactory INSTANCE = new MinioTestcontainerFactory();
    }
}
