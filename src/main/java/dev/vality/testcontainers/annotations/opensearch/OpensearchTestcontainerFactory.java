package dev.vality.testcontainers.annotations.opensearch;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpensearchTestcontainerFactory {

    private static final String OPENSEARCH_IMAGE_NAME = "opensearchproject/opensearch";
    private static final String TAG_PROPERTY = "testcontainers.opensearch.tag";

    private GenericContainer<?> opensearchContainer;

    public static GenericContainer<?> container() {
        return instance().create();
    }

    public static GenericContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static OpensearchTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private GenericContainer<?> getOrCreateSingletonContainer() {
        if (opensearchContainer != null) {
            return opensearchContainer;
        }
        opensearchContainer = create();
        return opensearchContainer;
    }

    private GenericContainer<?> create() {
        try (var container = new GenericContainer<>(
                DockerImageName
                        .parse(OPENSEARCH_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))) {
            container.withNetworkAliases("opensearch-" + UUID.randomUUID());
            container.withNetwork(Network.SHARED);
            container.withExposedPorts(9200, 9600);
            container.setWaitStrategy((new HttpWaitStrategy())
                    .forPort(9200)
                    .forStatusCodeMatching(response -> response == 200 || response == 401));
            container.withEnv("discovery.type", "single-node");
            container.withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true");
            container.withEnv("DISABLE_SECURITY_PLUGIN", "true");
            return container;
        }
    }

    private static class SingletonHolder {

        private static final OpensearchTestcontainerFactory INSTANCE = new OpensearchTestcontainerFactory();

    }
}
