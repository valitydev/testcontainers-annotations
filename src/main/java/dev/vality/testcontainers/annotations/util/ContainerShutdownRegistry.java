package dev.vality.testcontainers.annotations.util;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.lifecycle.Startable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class ContainerShutdownRegistry {

    private static final Set<Startable> CONTAINERS = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(
                ContainerShutdownRegistry::stopAll,
                "testcontainers-annotations-shutdown"));
    }

    private ContainerShutdownRegistry() {
    }

    public static <T extends Startable> T register(T container) {
        CONTAINERS.add(container);
        return container;
    }

    public static void unregister(Startable container) {
        CONTAINERS.remove(container);
    }

    private static void stopAll() {
        CONTAINERS.forEach(container -> {
            try {
                container.stop();
            } catch (RuntimeException ex) {
                log.warn("Unable to stop shared test container", ex);
            }
        });
        CONTAINERS.clear();
    }
}
