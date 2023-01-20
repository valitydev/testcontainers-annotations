package dev.vality.testcontainers.annotations.kafka.config;

import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Обертка над {@link KafkaTemplate}, используется для отправки сообщений в тестовый топик
 * <p>Пример использования {@link KafkaTestcontainer} с {@link KafkaProducer} — в
 * <a href="https://github.com/ValityDev/magista/tree/master/src/test/java/com/ValityDev/magista/config">magista</a>
 * <p><h3>Пример</h3>
 * <pre> {@code
 *     @Autowired
 *     private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
 *
 *     ...
 *
 *     testThriftKafkaProducer.send(invoicingTopicName, sinkEvent);
 *
 *     ...
 * }</pre>
 *
 * @see KafkaProducerConfig KafkaProducerConfig
 */
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public void send(String topic, T payload) {
        log.info("Sending payload='{}' to topic='{}'", payload, topic);
        kafkaTemplate.send(topic, payload)
                .join();
        kafkaTemplate.getProducerFactory().reset();
    }

    public void send(String topic, String key, T payload) {
        log.info("Sending key='{}' payload='{}' to topic='{}'", key, payload, topic);
        kafkaTemplate.send(topic, key, payload)
                .join();
        kafkaTemplate.getProducerFactory().reset();
    }

}
