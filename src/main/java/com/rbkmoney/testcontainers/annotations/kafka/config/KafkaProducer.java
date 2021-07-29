package com.rbkmoney.testcontainers.annotations.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
@Slf4j
public class KafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public void send(String topic, T payload) {
        log.info("Sending payload='{}' to topic='{}'", payload, topic);
        kafkaTemplate.send(topic, payload)
                .completable()
                .join();
    }
}
