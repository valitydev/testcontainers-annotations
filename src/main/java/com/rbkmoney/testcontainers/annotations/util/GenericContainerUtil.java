package com.rbkmoney.testcontainers.annotations.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericContainerUtil {

    public static void startContainer(GenericContainer<?> container) {
        Startables.deepStart(Stream.of(container))
                .join();
        assertThat(container.isRunning())
                .isTrue();
    }

    public static WaitStrategy getWaitStrategy(String path, Integer statusCode, Integer port, Duration duration) {
        return new HttpWaitStrategy()
                .forPath(path)
                .forPort(port)
                .forStatusCode(statusCode)
                .withStartupTimeout(duration);
    }
}
