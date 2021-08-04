package com.rbkmoney.testcontainers.annotations.kafka.config;

import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import com.rbkmoney.testcontainers.annotations.kafka.KafkaTestcontainer;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.thrift.TBase;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Листенер для чтения данных из тестового трифтового топика
 * Для получения конкретного сообщения необходимо имплементировать в тесте интерфейс
 * {@link MessageListener}
 * <p>Пример использования {@link KafkaTestcontainer} с {@link com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumer} — в
 * <a href="https://github.com/rbkmoney/sink-drinker/blob/master/src/test/java/com/rbkmoney/sinkdrinker/kafka/KafkaSenderTest.java">sink-drinker</a>
 * <p><h3>Пример</h3>
 * <pre> {@code
 *     @Autowired
 *     private KafkaConsumer<Event> testPayoutEventKafkaConsumer;
 *
 *     ...
 *
 *     testPayoutEventKafkaConsumer.read(topicName, data -> readEvents.add(data.value()));
 *     Unreliables.retryUntilTrue(TIMEOUT, TimeUnit.SECONDS, () -> readEvents.size() == expected);
 *
 *     ...
 * }</pre>
 *
 * @see KafkaConsumerConfig KafkaConsumerConfig
 */
@RequiredArgsConstructor
public class KafkaConsumer<T extends TBase<?, ?>> {

    private final String bootstrapAddress;
    private final AbstractThriftDeserializer<T> deserializer;

    public void read(String topic, MessageListener<String, T> messageListener) {
        var container = new ConcurrentMessageListenerContainer<>(
                consumerFactory(),
                containerProperties(topic, messageListener));
        container.start();
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
        return properties;
    }
}
