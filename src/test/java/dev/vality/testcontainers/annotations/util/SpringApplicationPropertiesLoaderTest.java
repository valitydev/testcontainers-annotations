package dev.vality.testcontainers.annotations.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringApplicationPropertiesLoaderTest {

    private static final String TOPIC_PROPERTY = "review.kafka.topic";

    @TempDir
    Path resources;

    @Test
    void shouldLoadActiveYamlDocumentAndProfileSpecificFile() throws Exception {
        Files.writeString(resources.resolve("application.yml"), """
                spring:
                  profiles:
                    active: review
                review:
                  kafka:
                    topic: base-topic
                ---
                spring:
                  config:
                    activate:
                      on-profile: review
                review:
                  kafka:
                    topic: profile-document-topic
                """);
        Files.writeString(resources.resolve("application-review.properties"),
                TOPIC_PROPERTY + "=profile-file-topic\n");

        withResourceClassLoader(() -> assertThat(
                SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile(List.of(TOPIC_PROPERTY))
                        .getProperty(TOPIC_PROPERTY))
                .isEqualTo("profile-file-topic"));
    }

    @Test
    void shouldPreferSystemProperty() throws Exception {
        Files.writeString(resources.resolve("application.properties"), TOPIC_PROPERTY + "=file-topic\n");
        System.setProperty(TOPIC_PROPERTY, "system-topic");
        try {
            withResourceClassLoader(() -> assertThat(
                    SpringApplicationPropertiesLoader.loadFromSpringApplicationPropertiesFile(List.of(TOPIC_PROPERTY))
                            .getProperty(TOPIC_PROPERTY))
                    .isEqualTo("system-topic"));
        } finally {
            System.clearProperty(TOPIC_PROPERTY);
        }
    }

    private void withResourceClassLoader(ThrowingRunnable action) throws Exception {
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        try (var classLoader = new URLClassLoader(new java.net.URL[] {resources.toUri().toURL()}, previous)) {
            thread.setContextClassLoader(classLoader);
            action.run();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}
