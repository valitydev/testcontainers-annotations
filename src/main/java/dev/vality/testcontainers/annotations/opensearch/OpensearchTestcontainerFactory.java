package dev.vality.testcontainers.annotations.opensearch;

import dev.vality.testcontainers.annotations.util.ContainerShutdownRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpensearchTestcontainerFactory {

    private static final String OPENSEARCH_IMAGE_NAME = "opensearchproject/opensearch";
    private static final String TAG_PROPERTY = "testcontainers.opensearch.tag";

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

    private static OpensearchTestcontainerFactory instance() {
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
        return new GenericContainer<>(DockerImageName.parse(OPENSEARCH_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withExposedPorts(9200, 9600)
                .waitingFor(new HttpWaitStrategy()
                        .forPort(9200)
                        .forStatusCodeMatching(response -> response == 200 || response == 401))
                .withEnv("discovery.type", "single-node")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .withEnv("DISABLE_SECURITY_PLUGIN", "true");
    }

    private static class SingletonHolder {

        private static final OpensearchTestcontainerFactory INSTANCE = new OpensearchTestcontainerFactory();
    }
}
