package com.rbkmoney.testcontainers.annotations;

import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumerConfig;
import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @KafkaSpringBootTest} представляет из себя
 * типичный для домена <a href="https://github.com/rbkmoney">rbkmoney</a>
 * набор аннотаций, используемых с {@link SpringBootTest} при тестировании спринговых приложений,
 * которые расположены в обертке {@link DefaultSpringBootTest}
 * и дополнительными подключенными конфигами
 * {@link KafkaProducerConfig} и {@link KafkaConsumerConfig}, которые содержат удобные инструменты
 * для тестирования консьюмеров и продъюсеров
 *
 * @see com.rbkmoney.testcontainers.annotations.kafka.KafkaTestcontainer @KafkaTestcontainer
 * @see com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducerConfig KafkaProducerConfig
 * @see com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumerConfig KafkaConsumerConfig
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@DefaultSpringBootTest
@ContextConfiguration(
        classes = {
                KafkaProducerConfig.class,
                KafkaConsumerConfig.class})
public @interface KafkaSpringBootTest {
}
