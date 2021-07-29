package com.rbkmoney.testcontainers.annotations.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericContainerUtil {

    public static void startContainer(GenericContainer<?> container) {
        Startables.deepStart(Stream.of(container))
                .join();
        assertThat(container.isRunning())
                .isTrue();
    }
}
