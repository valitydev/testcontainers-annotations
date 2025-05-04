package dev.vality.testcontainers.annotations.kafka;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class ConfluentKafkaContainer extends org.testcontainers.kafka.ConfluentKafkaContainer implements KafkaContainerExtension {

    private static final String CONFLUENT = "confluent";
    private static final String KAFKA_IMAGE_NAME = CONFLUENT + "inc/cp-kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka." + CONFLUENT + ".tag";
    private final List<String> topics;

    public ConfluentKafkaContainer(List<String> topics) {
        super(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        this.topics = topics;
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE", "false");
        withNetworkAliases("kafka-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    @Override
    public List<String> topics() {
        return topics;
    }

    @Override
    public String execInContainerKafkaTopicsListCommand() {
        var kafkaTopicsPath = "/usr/bin/kafka-topics";
        return execInContainerKafkaTopicsListCommandWithPath(kafkaTopicsPath);
    }
}
