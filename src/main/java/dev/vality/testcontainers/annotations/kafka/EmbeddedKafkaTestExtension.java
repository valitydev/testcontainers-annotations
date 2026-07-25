package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.util.TestExecutionLock;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EmbeddedKafkaTestExtension implements BeforeEachCallback {

    private static final int WAIT_TIMEOUT_SECONDS = 10;

    @Override
    public void beforeEach(ExtensionContext context) {
        AnnotationSupport.findAnnotation(context.getTestClass(), EmbeddedKafkaTest.class)
                .filter(EmbeddedKafkaTest::cleanupTopics)
                .ifPresent(annotation -> cleanupTopics(context, annotation));
    }

    private void cleanupTopics(ExtensionContext context, EmbeddedKafkaTest annotation) {
        var excludedTopics = Set.copyOf(Arrays.asList(annotation.excludeCleanupTopics()));
        var topics = Arrays.stream(annotation.topics())
                .filter(topic -> !excludedTopics.contains(topic))
                .toList();
        if (topics.isEmpty()) {
            return;
        }

        TestExecutionLock.acquire(context);
        try {
            var applicationContext = SpringExtension.getApplicationContext(context);
            var embeddedKafkaBroker = applicationContext.getBean(EmbeddedKafkaBroker.class);
            try (var adminClient = AdminClient.create(buildAdminProperties(embeddedKafkaBroker))) {
                var existingTopics = adminClient.listTopics().names().get(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var missingTopics = topics.stream()
                        .filter(topic -> !existingTopics.contains(topic))
                        .toList();
                if (!missingTopics.isEmpty()) {
                    embeddedKafkaBroker.addTopics(missingTopics.toArray(String[]::new));
                }
                var recordsToDelete = buildRecordsToDelete(embeddedKafkaBroker, topics);
                if (!recordsToDelete.isEmpty()) {
                    adminClient.deleteRecords(recordsToDelete).all().get(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            TestExecutionLock.release(context);
            throw new IllegalStateException("Interrupted while cleaning embedded Kafka topics", ex);
        } catch (ExecutionException | TimeoutException ex) {
            TestExecutionLock.release(context);
            throw new IllegalStateException("Unable to clean embedded Kafka topics", ex);
        } catch (RuntimeException ex) {
            TestExecutionLock.release(context);
            throw ex;
        }
    }

    private Map<TopicPartition, RecordsToDelete> buildRecordsToDelete(
            EmbeddedKafkaBroker embeddedKafkaBroker,
            List<String> topics) {
        try (var consumer = new KafkaConsumer<byte[], byte[]>(buildConsumerProperties(embeddedKafkaBroker))) {
            var topicPartitions = topics.stream()
                    .flatMap(topic -> buildTopicPartitions(topic, embeddedKafkaBroker.getPartitionsPerTopic()).stream())
                    .toList();
            consumer.assign(topicPartitions);
            var endOffsets = consumer.endOffsets(topicPartitions);
            var recordsToDelete = new HashMap<TopicPartition, RecordsToDelete>();
            endOffsets.forEach((topicPartition, offset) -> {
                if (offset > 0) {
                    recordsToDelete.put(topicPartition, RecordsToDelete.beforeOffset(offset));
                }
            });
            return recordsToDelete;
        }
    }

    private List<TopicPartition> buildTopicPartitions(String topic, int partitions) {
        return java.util.stream.IntStream.range(0, partitions)
                .mapToObj(partition -> new TopicPartition(topic, partition))
                .toList();
    }

    private Properties buildAdminProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        return properties;
    }

    private Properties buildConsumerProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
        var properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "embedded-kafka-cleanup-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return properties;
    }
}
