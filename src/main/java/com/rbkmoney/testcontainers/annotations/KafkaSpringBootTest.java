package com.rbkmoney.testcontainers.annotations;

import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumerConfig;
import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@DefaultSpringBootTest
@ContextConfiguration(
        classes = {
                KafkaProducerConfig.class,
                KafkaConsumerConfig.class})
public @interface KafkaSpringBootTest {
}
