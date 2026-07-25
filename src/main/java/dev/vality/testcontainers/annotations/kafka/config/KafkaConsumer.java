package dev.vality.testcontainers.annotations.kafka.config;

import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.thrift.TBase;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener helper for reading records from a test Thrift topic.
 */
@RequiredArgsConstructor
public class KafkaConsumer<T extends TBase<?, ?>> implements DisposableBean, AutoCloseable {

    private final String bootstrapAddress;
    private final AbstractThriftDeserializer<T> deserializer;
    private final Set<ConcurrentMessageListenerContainer<String, T>> containers = ConcurrentHashMap.newKeySet();

    /**
     * Starts a listener container managed by this bean.
     */
    public void read(String topic, MessageListener<String, T> messageListener) {
        start(topic, messageListener);
    }

    /**
     * Starts a listener container and returns a handle that can be stopped explicitly.
     */
    public ConcurrentMessageListenerContainer<String, T> start(
            String topic,
            MessageListener<String, T> messageListener) {
        var container = new ConcurrentMessageListenerContainer<>(
                consumerFactory(),
                containerProperties(topic, messageListener));
        containers.add(container);
        try {
            container.start();
            return container;
        } catch (RuntimeException ex) {
            try {
                container.stop();
                containers.remove(container);
            } catch (RuntimeException stopException) {
                ex.addSuppressed(stopException);
            }
            throw ex;
        }
    }

    public void stop(ConcurrentMessageListenerContainer<String, T> container) {
        if (container != null && containers.contains(container)) {
            container.stop();
            containers.remove(container);
        }
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        for (var container : Set.copyOf(containers)) {
            try {
                container.stop();
                containers.remove(container);
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new IllegalStateException("Unable to stop Kafka listener containers");
                }
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private ContainerProperties containerProperties(String topic, MessageListener<String, T> messageListener) {
        var containerProperties = new ContainerProperties(topic);
        containerProperties.setMessageListener(messageListener);
        return containerProperties;
    }

    private DefaultKafkaConsumerFactory<String, T> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfig(), new StringDeserializer(), deserializer);
    }

    private Map<String, Object> consumerConfig() {
        var properties = new HashMap<String, Object>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return properties;
    }
}
