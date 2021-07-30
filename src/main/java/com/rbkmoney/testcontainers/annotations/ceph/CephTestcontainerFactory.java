package com.rbkmoney.testcontainers.annotations.ceph;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

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
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withNetworkAliases("ceph-daemon")
                .withEnv("RGW_NAME", "localhost")
                .withEnv("NETWORK_AUTO_DETECT", "4")
                .withEnv("CEPH_DAEMON", "demo")
                .withEnv("CEPH_DEMO_UID", "ceph-test")
                .withEnv("CEPH_DEMO_ACCESS_KEY", loadDefaultLibraryProperty(ACCESS_KEY))
                .withEnv("CEPH_DEMO_SECRET_KEY", loadDefaultLibraryProperty(SECRET_KEY))
                .withEnv("CEPH_DEMO_BUCKET", "TEST")
                .waitingFor(cephHealthCheck())) {
            return container;
        }
    }

    private WaitStrategy cephHealthCheck() {
        return getWaitStrategy("/api/v0.1/health", 200, 5000, Duration.ofMinutes(1));
    }

    private static WaitStrategy getWaitStrategy(String path, Integer statusCode, Integer port, Duration duration) {
        return new HttpWaitStrategy()
                .forPath(path)
                .forPort(port)
                .forStatusCode(statusCode)
                .withStartupTimeout(duration);
    }

    private static class SingletonHolder {

        private static final CephTestcontainerFactory INSTANCE = new CephTestcontainerFactory();

    }
}
