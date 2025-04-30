package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.exception.KafkaStartingException;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class ConfluentKafkaContainer extends org.testcontainers.kafka.ConfluentKafkaContainer implements KafkaContainerExtension {

    private static final String CONFLUENT = "confluentinc";
    private static final String KAFKA_IMAGE_NAME = CONFLUENT + "/cp-kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka." + CONFLUENT + ".tag";

    public ConfluentKafkaContainer() {
        super(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE", "false");
    }

    @Override
    public String execInContainerKafkaTopicsListCommand() {
        var kafkaTopicsListCommand = "/usr/bin/kafka-topics --bootstrap-server localhost:9093 --list";
        try {
            var stdout = execInContainer("/bin/bash", "-c", kafkaTopicsListCommand)
                    .getStdout();
            log.info("Topics list from '/opt/kafka/bin/kafka-topics.sh': [{}]", stdout.replace("\n", ","));
            return stdout;
        } catch (IOException ex) {
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        }
    }
}
