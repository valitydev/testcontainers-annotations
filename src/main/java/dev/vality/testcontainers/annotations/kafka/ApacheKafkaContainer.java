package dev.vality.testcontainers.annotations.kafka;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class ApacheKafkaContainer extends KafkaContainer implements KafkaContainerExtension {

    private static final String APACHE = "apache";
    private static final String KAFKA_IMAGE_NAME = APACHE + "/kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka." + APACHE + ".tag";

    public ApacheKafkaContainer() {
        super(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE", "false");
        withNetworkAliases("kafka-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    @Override
    public String execInContainerKafkaTopicsListCommand() {
        var kafkaTopicsPath = "/opt/kafka/bin/kafka-topics.sh";
        return execInContainerKafkaTopicsListCommand(kafkaTopicsPath);
    }
}
