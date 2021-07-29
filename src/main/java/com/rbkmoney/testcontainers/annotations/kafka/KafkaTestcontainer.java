package com.rbkmoney.testcontainers.annotations.kafka;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(KafkaTestcontainerExtension.class)
public @interface KafkaTestcontainer {

    /**
     * properties = {"kafka.topics.invoicing.consume.enabled=true",...}
     */
    String[] properties() default {};

    /**
     * topicsKeys = {"kafka.topics.invoicing.id",...}
     */
    String[] topicsKeys();

}
