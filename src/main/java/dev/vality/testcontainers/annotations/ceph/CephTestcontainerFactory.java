package dev.vality.testcontainers.annotations.ceph;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 * <p>{@link #ACCESS_KEY} необходимо указать в файле application.yml при необходимости другого ключа
 * <p>{@link #SECRET_KEY} необходимо указать в файле application.yml при необходимости другого ключа
 *
 * @see CephTestcontainerExtension CephTestcontainerExtension
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CephTestcontainerFactory {

    public static final String ACCESS_KEY = "testcontainers.ceph.accessKey";
    public static final String SECRET_KEY = "testcontainers.ceph.secretKey";
    private static final String CEPH_DAEMON_IMAGE_NAME = "ceph/daemon";
    private static final String TAG_PROPERTY = "testcontainers.ceph.tag";

    private GenericContainer<?> cephDaemonContainer;

    public static GenericContainer<?> container() {
        return instance().create();
    }

    public static GenericContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static CephTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private GenericContainer<?> getOrCreateSingletonContainer() {
        if (cephDaemonContainer != null) {
            return cephDaemonContainer;
        }
        cephDaemonContainer = create();
        return cephDaemonContainer;
    }

    private GenericContainer<?> create() {
        try (GenericContainer<?> container = new GenericContainer<>(
                DockerImageName
                        .parse(CEPH_DAEMON_IMAGE_NAME)
                        .withTag(SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withExposedPorts(5000, 8080)
                .withNetworkAliases("ceph-daemon-" + UUID.randomUUID())
                .withEnv("RGW_NAME", "localhost")
                .withEnv("NETWORK_AUTO_DETECT", "4")
                .withEnv("CEPH_DAEMON", "demo")
                .withEnv("CEPH_DEMO_UID", "ceph-test")
                .withEnv("CEPH_DEMO_ACCESS_KEY", SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(ACCESS_KEY))
                .withEnv("CEPH_DEMO_SECRET_KEY", SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(SECRET_KEY))
                .withEnv("CEPH_DEMO_BUCKET", "TEST")
                .waitingFor(GenericContainerUtil.getWaitStrategy("/api/v0.1/health", 200, 5000, Duration.ofMinutes(1)))) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final CephTestcontainerFactory INSTANCE = new CephTestcontainerFactory();

    }
}
