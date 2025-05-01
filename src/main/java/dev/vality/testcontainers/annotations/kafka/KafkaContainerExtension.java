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

    String getBootstrapServers();

    default void createTopics(List<String> topics) {
        try (var admin = createAdminClient()) {
            var newTopics = topics.stream()
                    .map(topic -> new NewTopic(topic, 1, (short) 1))
                    .peek(newTopic -> log.info(newTopic.toString()))
                    .collect(Collectors.toList());
            var topicsResult = admin.createTopics(newTopics);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> topicsResult.all().get(1, TimeUnit.SECONDS));
            var adminClientTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS CREATED]: {}", adminClientTopics);
            assertThat(adminClientTopics.size())
                    .isEqualTo(topics.size());
            assertThat(execInContainerKafkaTopicsListCommand())
                    .contains(topics);
        } catch (ExecutionException | TimeoutException ex) {
            throw new KafkaStartingException("Error when topic creating, ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when topic creating, ", ex);
        }
    }

    default void deleteTopics(List<String> topics) {
        try (var admin = createAdminClient()) {
            var topicsResult = admin.deleteTopics(topics);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(WAIT_TIMEOUT))
                    .pollInterval(Duration.ofSeconds(2))
                    .untilAsserted(() -> topicsResult.all().get(1, TimeUnit.SECONDS));
            var adminClientTopics = admin.listTopics().names().get(WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("Topics list from 'AdminClient' after [TOPICS DELETED]: {} (should be empty)", adminClientTopics);
            assertThat(adminClientTopics)
                    .isEmpty();
            execInContainerKafkaTopicsListCommand();
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

    String execInContainerKafkaTopicsListCommand();

    default String execInContainerKafkaTopicsListCommand(String kafkaTopicsPath) {
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
