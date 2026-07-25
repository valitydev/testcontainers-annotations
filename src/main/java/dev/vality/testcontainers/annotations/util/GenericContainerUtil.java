package dev.vality.testcontainers.annotations.util;

import dev.vality.testcontainers.annotations.kafka.KafkaContainerExtension;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public class GenericContainerUtil {

    public static void startContainer(GenericContainer<?> container) {
        start(container);
    }

    public static void startContainer(KafkaContainerExtension container) {
        start(container);
    }

    private static <T extends Startable & ContainerState> void start(T container) {
        synchronized (container) {
            if (container.isRunning()) {
                return;
            }
            try {
                Startables.deepStart(Stream.of(container)).join();
            } catch (CompletionException ex) {
                throw new IllegalStateException("Unable to start test container", ex.getCause());
            }
            if (!container.isRunning()) {
                throw new IllegalStateException("Test container did not reach the running state");
            }
        }
    }

    public static WaitStrategy getWaitStrategy(String path, Integer statusCode, Integer port, Duration duration) {
        return new HttpWaitStrategy()
                .forPath(path)
                .forPort(port)
                .forStatusCode(statusCode)
                .withStartupTimeout(duration);
    }
}
