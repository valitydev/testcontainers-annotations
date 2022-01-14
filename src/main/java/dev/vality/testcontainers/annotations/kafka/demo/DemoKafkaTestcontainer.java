package dev.vality.testcontainers.annotations.kafka.demo;

import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainer;

/**
 * this is a demo example of filling in annotation, do not use
 */
@KafkaTestcontainer(
        properties = "kafka.topics.invoicing.consume.enabled=true",
        topicsKeys = "kafka.topics.invoicing.id")
public @interface DemoKafkaTestcontainer {
}
