package dev.vality.testcontainers.annotations.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import dev.vality.testcontainers.annotations.exception.KafkaStartingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerExtension.WAIT_TIMEOUT;
import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class BitnamiKafkaContainer extends GenericContainer<BitnamiKafkaContainer> implements KafkaContainerExtension {

    private static final String BITNAMI = "bitnami";
    private static final String KAFKA_IMAGE_NAME = BITNAMI + "/kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka." + BITNAMI + ".tag";
    private static final int KAFKA_PORT = 9092;
    private static final int CONTROLLER_PORT = 9093;
    public static final int INTERNAL = 29092;

    public BitnamiKafkaContainer() {
        super(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withExposedPorts(KAFKA_PORT);
        withCommand("sleep", "infinity");
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_CFG_PROCESS_ROLES", "broker,controller");
        withEnv("KAFKA_CFG_NODE_ID", "1");
        withEnv("KAFKA_CFG_CONTROLLER_QUORUM_VOTERS", "1@localhost:" + CONTROLLER_PORT);
        withEnv("KAFKA_CFG_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
        withEnv("KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT");
        withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:" + KAFKA_PORT);
        withEnv("KAFKA_CFG_LISTENERS", "PLAINTEXT://:" + KAFKA_PORT + ",CONTROLLER://:" + CONTROLLER_PORT + ",INTERNAL://:" + INTERNAL);
        withEnv("KAFKA_CFG_BROKER_ID", "1");
        withEnv("KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_NUM_PARTITIONS", "1");
        withEnv("KAFKA_CFG_LOG_RETENTION_HOURS", "1");
        withEnv("KAFKA_CFG_LOG_SEGMENT_BYTES", "1048576");
        withEnv("KAFKA_CFG_MIN_INSYNC_REPLICAS", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE", "false");
        waitingFor(new WaitAllStrategy()
                .withStrategy(Wait.forLogMessage(".*Welcome to the Bitnami kafka container.*", 1))
                .withStrategy(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofSeconds(WAIT_TIMEOUT)));
    }

    @Override
    @SneakyThrows
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);
        var advertisedListeners = String.join(",",
                "PLAINTEXT://" + getHost() + ":" + getMappedPort(KAFKA_PORT),
                "INTERNAL://localhost:" + INTERNAL);
        var script = String.join("\n",
                "#!/bin/bash",
                "export KAFKA_CFG_ADVERTISED_LISTENERS=" + advertisedListeners,
                "/opt/bitnami/scripts/kafka/entrypoint.sh /opt/bitnami/scripts/kafka/run.sh > /tmp/kafka.log 2>&1");
        copyFileToContainer(Transferable.of(script.getBytes(), 0777), "/start-kafka.sh");
        execInContainer("sh", "-c", "nohup /start-kafka.sh &");
        execInContainer("sh", "-c", "tail -n+1 -f /tmp/kafka.log > /proc/1/fd/1 &");
        WaitingConsumer logConsumer = new WaitingConsumer();
        followOutput(logConsumer);
        logConsumer.waitUntil(
                frame -> frame.getUtf8String().contains("Kafka startTimeMs"),
                60, TimeUnit.SECONDS);
    }

    @Override
    public String getBootstrapServers() {
        return getHost() + ":" + getMappedPort(KAFKA_PORT);
    }

    @Override
    public String execInContainerKafkaTopicsListCommand() {
        var kafkaTopicsListCommand = "/opt/" + BITNAMI + "/kafka/bin/kafka-topics.sh --bootstrap-server INTERNAL://localhost:" + INTERNAL + " --list";
        try {
            var stdout = execInContainer("/bin/bash", "-c", kafkaTopicsListCommand)
                    .getStdout();
            log.info("Topics list from '/opt/" + BITNAMI + "/kafka/bin/kafka-topics.sh': [{}]", stdout.replace("\n", ","));
            return stdout;
        } catch (IOException ex) {
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaStartingException("Error when " + kafkaTopicsListCommand + ", ", ex);
        }
    }
}
