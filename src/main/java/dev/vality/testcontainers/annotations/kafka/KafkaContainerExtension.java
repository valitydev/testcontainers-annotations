package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.exception.KafkaStartingException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.lifecycle.Startable;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface KafkaContainerExtension extends Startable, ContainerState {

    int WAIT_TIMEOUT = 90;

    Logger log = LoggerFactory.getLogger(KafkaContainerExtension.class);

    List<String> topics();

    String getBootstrapServers();

    String execInContainerKafkaTopicsListCommand();

    default void createTopics(List<String> excludedTopics) {
        var topicsToEnsure = topics().stream()
                .filter(topic -> !excludedTopics.contains(topic))
                .toList();
        if (topicsToEnsure.isEmpty()) {
            return;
        }

        try (var admin = createAdminClient()) {
            var existingTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            var topicsToCreate = topicsToEnsure.stream()
                    .filter(topic -> !existingTopics.contains(topic))
                    .toList();
            if (!topicsToCreate.isEmpty()) {
                var newTopics = topicsToCreate.stream()
                        .map(topic -> new NewTopic(topic, 1, (short) 1))
                        .toList();
                admin.createTopics(newTopics).all().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(1))
                    .until(() -> {
                        var actual = admin.listTopics().names().get(5, TimeUnit.SECONDS);
                        return actual.containsAll(topicsToEnsure);
                    });

            var actual = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            if (!actual.containsAll(topicsToEnsure)) {
                throw new KafkaStartingException(
                        "Kafka topics were not created. Expected=" + topicsToEnsure + ", actual=" + actual,
                        null);
            }
            log.info("Kafka topics are ready: {}", topicsToEnsure);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error while creating Kafka topics", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Interrupted while creating Kafka topics", ex);
        }
    }

    default void deleteTopics(List<String> excludedTopics) {
        try (var admin = createAdminClient()) {
            var existingTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            var topicsToDelete = topics().stream()
                    .filter(topic -> !excludedTopics.contains(topic))
                    .filter(existingTopics::contains)
                    .toList();
            if (topicsToDelete.isEmpty()) {
                return;
            }

            admin.deleteTopics(topicsToDelete).all().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(1))
                    .until(() -> {
                        var actual = admin.listTopics().names().get(5, TimeUnit.SECONDS);
                        return actual.stream().noneMatch(topicsToDelete::contains);
                    });

            var actual = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            var remaining = topicsToDelete.stream().filter(actual::contains).toList();
            if (!remaining.isEmpty()) {
                throw new KafkaStartingException("Kafka topics were not deleted: " + remaining, null);
            }
            log.info("Kafka topics were deleted: {}", topicsToDelete);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error while deleting Kafka topics", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Interrupted while deleting Kafka topics", ex);
        }
    }

    default AdminClient createAdminClient() {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        return AdminClient.create(properties);
    }

    default String execInContainerKafkaTopicsListCommandWithPath(String kafkaTopicsPath) {
        var command = kafkaTopicsPath + " --bootstrap-server localhost:9093 --list";
        try {
            var result = execInContainer("/bin/bash", "-c", command);
            if (result.getExitCode() != 0) {
                throw new KafkaStartingException(
                        "Kafka topics command failed with exit code " + result.getExitCode() +
                                ": " + result.getStderr(),
                        null);
            }
            log.info("Topics list from '{}': {}", kafkaTopicsPath, result.getStdout().replace("\n", ","));
            return result.getStdout();
        } catch (IOException ex) {
            throw new KafkaStartingException("Error while executing " + command, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Interrupted while executing " + command, ex);
        }
    }

    default Set<String> parseTopicList(String stdout) {
        return Arrays.stream(stdout.split("\\R"))
                .map(String::trim)
                .filter(topic -> !topic.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
