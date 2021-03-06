package dev.vality.testcontainers.annotations.kafka.config;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.thrift.TBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Конфиг для инциализации тестового продьюссера для тестирования трифтовых топиков
 *
 * @see KafkaProducer KafkaProducer
 */
@TestConfiguration
public class KafkaProducerConfig {

    @Bean
    public String bootstrapAddress(@Value("${spring.kafka.bootstrap-servers:}") String primaryLocation,
                                   @Value("${kafka.bootstrap-servers:}") String secondaryLocation) {
        return !ObjectUtils.isEmpty(primaryLocation) ? primaryLocation : secondaryLocation;
    }

    @Bean
    public KafkaProducer<TBase<?, ?>> testThriftKafkaProducer(String bootstrapAddress) {
        return new KafkaProducer<>(new KafkaTemplate<>(thriftProducerFactory(bootstrapAddress)));
    }

    private ProducerFactory<String, TBase<?, ?>> thriftProducerFactory(String bootstrapAddress) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(props);
    }
}
