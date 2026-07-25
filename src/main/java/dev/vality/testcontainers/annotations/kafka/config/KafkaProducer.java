package dev.vality.testcontainers.annotations.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Обертка над {@link KafkaTemplate}, используется для отправки сообщений в тестовый топик.
 */
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public void send(String topic, T payload) {
        log.info("Sending payload='{}' to topic='{}'", payload, topic);
        kafkaTemplate.send(topic, payload).join();
    }

    public void send(String topic, String key, T payload) {
        log.info("Sending key='{}' payload='{}' to topic='{}'", key, payload, topic);
        kafkaTemplate.send(topic, key, payload).join();
    }
}
