package dev.vality.testcontainers.annotations;

import dev.vality.testcontainers.annotations.kafka.config.KafkaConsumerConfig;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @KafkaSpringBootTest} представляет из себя
 * типичный для домена <a href="https://github.com/ValityDev">ValityDev</a>
 * набор аннотаций, используемых с {@link SpringBootTest} при тестировании спринговых приложений,
 * которые расположены в обертке {@link DefaultSpringBootTest}
 * и дополнительными подключенными конфигами
 * {@link KafkaProducerConfig} и {@link KafkaConsumerConfig}, которые содержат удобные инструменты
 * для тестирования консьюмеров и продъюсеров
 *
 * @see KafkaTestcontainer @KafkaTestcontainer
 * @see KafkaProducerConfig KafkaProducerConfig
 * @see KafkaConsumerConfig KafkaConsumerConfig
 * @see DefaultSpringBootTest @DefaultSpringBootTest
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
