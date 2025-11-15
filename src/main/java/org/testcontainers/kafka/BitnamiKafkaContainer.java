package org.testcontainers.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import dev.vality.testcontainers.annotations.kafka.KafkaContainerExtension;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("LineLength")
@Deprecated
public class BitnamiKafkaContainer extends GenericContainer<BitnamiKafkaContainer> implements KafkaContainerExtension {

    private static final int KAFKA_PORT = 9092;
    private static final int BROKER_PORT = 9093;
    private static final int LIMIT = 120;
    private final List<String> topics;

    public BitnamiKafkaContainer(DockerImageName dockerImageName, List<String> topics) {
        super(dockerImageName);
        this.topics = topics;
        dockerImageName.assertCompatibleWith(DockerImageName.parse("bitnami/kafka"));
        withExposedPorts(KafkaHelper.KAFKA_PORT);
        withEnv(KafkaHelper.envVars());
        withEnv("KAFKA_CFG_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_CFG_DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        withEnv("DELETE_TOPIC_ENABLE", "true");
        withEnv("KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE", "false");
        withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        withEnv("KAFKA_CFG_NODE_ID", "1");
        withCommand(KafkaHelper.COMMAND);
        waitingFor(new WaitAllStrategy()
                .withStrategy(Wait.forLogMessage(".*Welcome to the Bitnami kafka container.*", 1))
                .withStrategy(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofSeconds(LIMIT)));
        withNetworkAliases("kafka-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    // copy-paste from org.testcontainers.kafka.KafkaContainer + /tmp/kafka.log WaitingConsumer
    @Override
    @SneakyThrows
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        var brokerAdvertisedListener = String.format(
                "BROKER://%s:%s",
                containerInfo.getConfig().getHostName(),
                BROKER_PORT
        );
        var advertisedListeners = new ArrayList<String>();
        advertisedListeners.add("PLAINTEXT://" + getBootstrapServers());
        advertisedListeners.add(brokerAdvertisedListener);
        var kafkaAdvertisedListeners = String.join(",", advertisedListeners);
        var scriptsPath = "/opt/bitnami/scripts/kafka/";
        var script = String.join("\n",
                "#!/bin/bash",
                "export KAFKA_CFG_ADVERTISED_LISTENERS=" + kafkaAdvertisedListeners,
                scriptsPath + "entrypoint.sh " + scriptsPath + "run.sh > /tmp/kafka.log 2>&1");
        copyFileToContainer(Transferable.of(script.getBytes(), 511), "/start-kafka.sh");
        execInContainer("sh", "-c", "nohup /start-kafka.sh &");
        execInContainer("sh", "-c", "tail -n+1 -f /tmp/kafka.log > /proc/1/fd/1 &");
        var logConsumer = new WaitingConsumer();
        followOutput(logConsumer);
        logConsumer.waitUntil(frame -> frame.getUtf8String().contains("Kafka startTimeMs"), LIMIT, TimeUnit.SECONDS);
    }

    @Override
    public List<String> topics() {
        return topics;
    }

    @Override
    public String getBootstrapServers() {
        return getHost() + ":" + getMappedPort(KAFKA_PORT);
    }

    @Override
    public String execInContainerKafkaTopicsListCommand() {
        var kafkaTopicsPath = "/opt/bitnami/kafka/bin/kafka-topics.sh";
        return execInContainerKafkaTopicsListCommandWithPath(kafkaTopicsPath);
    }
}
