package dev.vality.testcontainers.annotations.kafka;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class BitnamiKafkaContainer extends org.testcontainers.kafka.BitnamiKafkaContainer implements KafkaContainerExtension {

    private static final String BITNAMI = "bitnami";
    private static final String KAFKA_IMAGE_NAME = BITNAMI + "/kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka." + BITNAMI + ".tag";

    public BitnamiKafkaContainer() {
        super(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
    }
}
