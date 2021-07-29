package com.rbkmoney.testcontainers.annotations.spring.boot.test.context;

import com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application.yml")
@DirtiesContext
@ContextConfiguration(classes = KafkaConsumerConfig.class)
public @interface KafkaConsumerSpringBootTest {
}
