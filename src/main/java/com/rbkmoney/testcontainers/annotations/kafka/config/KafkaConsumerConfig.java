package com.rbkmoney.testcontainers.annotations.kafka.config;

import com.rbkmoney.damsel.fraudbusters.Command;
import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payout.manager.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public KafkaConsumer<SinkEvent> testSinkEventKafkaConsumer() {
        return new KafkaConsumer<>(bootstrapAddress, new SinkEventDeserializer());
    }

    @Bean
    public KafkaConsumer<Event> testPayoutEventKafkaConsumer() {
        return new KafkaConsumer<>(bootstrapAddress, new PayoutEventDeserializer());
    }

    @Bean
    public KafkaConsumer<Command> testFraudbustersCommandKafkaConsumer() {
        return new KafkaConsumer<>(bootstrapAddress, new FraudbustersCommandDeserializer());
    }

    public static class SinkEventDeserializer extends AbstractThriftDeserializer<SinkEvent> {

        @Override
        public SinkEvent deserialize(String s, byte[] bytes) {
            return super.deserialize(bytes, new SinkEvent());
        }
    }

    public static class PayoutEventDeserializer extends AbstractThriftDeserializer<Event> {

        @Override
        public Event deserialize(String s, byte[] bytes) {
            return super.deserialize(bytes, new Event());
        }
    }

    public static class FraudbustersCommandDeserializer extends AbstractThriftDeserializer<Command> {

        @Override
        public Command deserialize(String s, byte[] bytes) {
            return super.deserialize(bytes, new Command());
        }
    }
}
