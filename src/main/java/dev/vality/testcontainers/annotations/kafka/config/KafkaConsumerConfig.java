package dev.vality.testcontainers.annotations.kafka.config;

import com.rbkmoney.machinegun.eventsink.SinkEvent;
import dev.vality.damsel.fraudbusters.Command;
import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.payout.manager.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Конфиг для инциализации тестового консьюмера для тестирования трифтовых топиков
 * В данный момент поддерживаются
 * {@link SinkEvent}
 * {@link Event}
 * {@link Command}
 *
 * @see KafkaConsumer KafkaConsumer
 */
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
