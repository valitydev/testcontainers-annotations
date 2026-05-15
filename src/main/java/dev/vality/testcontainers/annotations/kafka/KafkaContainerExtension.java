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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public interface KafkaContainerExtension extends Startable, ContainerState {

    int WAIT_TIMEOUT = 90;

    Logger log = LoggerFactory.getLogger(KafkaContainerExtension.class);

    List<String> topics();

    String getBootstrapServers();

    String execInContainerKafkaTopicsListCommand();

    default void createTopics(List<String> excludedTopics) {
        try (var admin = createAdminClient()) {
            var topics = topics().stream()
                    .filter(topic -> !excludedTopics.contains(topic))
                    .toList();
            if (topics.isEmpty()) {
                return;
            }
            var existingTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            var topicsToCreate = topics.stream()
                    .filter(topic -> !existingTopics.contains(topic))
                    .toList();
            if (topicsToCreate.isEmpty()) {
                return;
            }
            var newTopics = topicsToCreate.stream()
                    .map(topic -> new NewTopic(topic, 1, (short) 1))
                    .peek(newTopic -> log.info(newTopic.toString()))
                    .collect(Collectors.toList());
            var topicsResult = admin.createTopics(newTopics);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> topicsResult.all().get(1, TimeUnit.SECONDS));
            var topicsAfterCreate = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS CREATED]: {}", topicsAfterCreate);
            assertThat(topicsAfterCreate)
                    .containsAll(topicsToCreate);
            var actual = execInContainerKafkaTopicsListCommand();
            assertThat(topicsToCreate.stream().allMatch(actual::contains))
                    .isTrue();
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic creating, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic creating, ", ex);
        }
    }

    default void deleteTopics(List<String> excludedTopics) {
        try (var admin = createAdminClient()) {
            var existingTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            if (existingTopics.isEmpty()) {
                return;
            }
            var topics = topics().stream()
                    .filter(topic -> !excludedTopics.contains(topic))
                    .toList();
            var topicsToDelete = topics.stream()
                    .filter(existingTopics::contains)
                    .toList();
            if (topicsToDelete.isEmpty()) {
                return;
            }
            var topicsResult = admin.deleteTopics(topicsToDelete);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> topicsResult.all().get(1, TimeUnit.SECONDS));
            var topicsAfterDelete = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS DELETED]: {}", topicsAfterDelete);
            assertThat(topicsAfterDelete.stream().noneMatch(topicsToDelete::contains))
                    .isTrue();
            var actual = execInContainerKafkaTopicsListCommand();
            assertThat(topicsToDelete.stream().noneMatch(actual::contains))
                    .isTrue();
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic deleting, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic deleting, ", ex);
        }
    }

    default AdminClient createAdminClient() {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        return AdminClient.create(properties);
    }

    default String execInContainerKafkaTopicsListCommandWithPath(String kafkaTopicsPath) {
        var kafkaTopicsListCommand = kafkaTopicsPath + " --bootstrap-server localhost:9093 --list";
        try {
            var stdout = execInContainer("/bin/bash", "-c", kafkaTopicsListCommand).getStdout();
            log.info("Topics list from '{}': [{}]", kafkaTopicsPath, stdout.replace("\n", ","));
            return stdout;
        } catch (IOException ex) {
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        }
    }
}
