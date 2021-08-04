package com.rbkmoney.testcontainers.annotations.kafka.config;

import com.rbkmoney.testcontainers.annotations.kafka.KafkaTestcontainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Обертка над {@link KafkaTemplate}, используется для отправки сообщений в тестовый топик
 * <p>Пример использования {@link KafkaTestcontainer} с {@link KafkaProducer} — в
 * <a href="https://github.com/rbkmoney/magista/tree/master/src/test/java/com/rbkmoney/magista/config">magista</a>
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
                .completable()
                .join();
        kafkaTemplate.getProducerFactory().reset();
    }
}
