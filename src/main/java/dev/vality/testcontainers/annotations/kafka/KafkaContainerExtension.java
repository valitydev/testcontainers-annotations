package dev.vality.testcontainers.annotations.kafka;

import org.testcontainers.containers.ContainerState;
import org.testcontainers.lifecycle.Startable;

public interface KafkaContainerExtension extends Startable, ContainerState {

    String getBootstrapServers();

    String execInContainerKafkaTopicsListCommand();

}
