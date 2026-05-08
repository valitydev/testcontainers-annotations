package dev.vality.testcontainers.annotations.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafkaTest(
        topics = EmbeddedKafkaTestIntegrationTest.TOPIC,
        properties = {
                "app.kafka.topic=" + EmbeddedKafkaTestIntegrationTest.TOPIC,
                "spring.kafka.consumer.group-id=embedded-kafka-test"
        }
)
@SpringBootTest(classes = EmbeddedKafkaTestIntegrationTest.Config.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmbeddedKafkaTestIntegrationTest {

    static final String TOPIC = "embedded-kafka-test-topic";

    @Autowired
    private Environment environment;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    @Order(1)
    void shouldStartEmbeddedKafkaAndExposeBootstrapProperties() throws Exception {
        var bootstrapServers = environment.getRequiredProperty("spring.kafka.bootstrap-servers");

        assertThat(bootstrapServers)
                .isEqualTo(embeddedKafkaBroker.getBrokersAsString());
        assertThat(environment.getRequiredProperty("kafka.bootstrap-servers"))
                .isEqualTo(embeddedKafkaBroker.getBrokersAsString());
        assertThat(environment.getRequiredProperty("kafka.ssl.enabled"))
                .isEqualTo("false");
        assertThat(environment.getRequiredProperty("app.kafka.topic"))
                .isEqualTo(TOPIC);

        try (var adminClient = AdminClient.create(adminProperties(bootstrapServers))) {
            assertThat(adminClient.listTopics().names().get(10, TimeUnit.SECONDS))
                    .contains(TOPIC);
        }

        try (var producer = new KafkaProducer<String, String>(producerProperties(bootstrapServers))) {
            producer.send(new ProducerRecord<>(TOPIC, "key", "value")).get(10, TimeUnit.SECONDS);
            producer.flush();
        }

        assertThat(readValues("first-reader", bootstrapServers))
                .contains("value");
    }

    @Test
    @Order(2)
    void shouldCleanupTopicsBeforeEachTest() {
        var bootstrapServers = environment.getRequiredProperty("spring.kafka.bootstrap-servers");

        assertThat(readValues("second-reader", bootstrapServers))
                .isEmpty();
    }

    private Properties adminProperties(String bootstrapServers) {
        var properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }

    private Properties producerProperties(String bootstrapServers) {
        var properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        return properties;
    }

    private List<String> readValues(String groupId, String bootstrapServers) {
        try (var consumer = new KafkaConsumer<String, String>(consumerProperties(groupId, bootstrapServers))) {
            consumer.subscribe(List.of(TOPIC));
            var values = new ArrayList<String>();
            for (var record : consumer.poll(Duration.ofMillis(500)).records(TOPIC)) {
                values.add(record.value());
            }
            return values;
        }
    }

    private Properties consumerProperties(String groupId, String bootstrapServers) {
        var properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    @Configuration
    static class Config {
    }
}
