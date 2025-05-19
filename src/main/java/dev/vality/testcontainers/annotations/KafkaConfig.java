package dev.vality.testcontainers.annotations;

import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(classes = {KafkaProducerConfig.class})
public @interface KafkaConfig {
}
