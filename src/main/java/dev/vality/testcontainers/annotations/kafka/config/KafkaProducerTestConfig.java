package dev.vality.testcontainers.annotations.kafka.config;

import dev.vality.kafka.common.serialization.ThriftSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.thrift.TBase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Конфигурация тестового producer для Thrift сообщений.
 */
@TestConfiguration("kafkaProducerTestContainersAnnotationsConfig")
public class KafkaProducerTestConfig {

    public static final String BOOTSTRAP_ADDRESS_BEAN = "testKafkaBootstrapAddress";
    public static final String PRODUCER_FACTORY_BEAN = "testThriftKafkaProducerFactory";
    public static final String KAFKA_TEMPLATE_BEAN = "testThriftKafkaTemplate";

    @Bean(BOOTSTRAP_ADDRESS_BEAN)
    public String bootstrapAddress(
            @Value("${spring.kafka.bootstrap-servers:}") String primaryLocation,
            @Value("${kafka.bootstrap-servers:}") String secondaryLocation) {
        var address = StringUtils.hasText(primaryLocation) ? primaryLocation : secondaryLocation;
        if (!StringUtils.hasText(address)) {
            throw new IllegalStateException("Kafka bootstrap servers are not configured");
        }
        return address;
    }

    @Bean(value = PRODUCER_FACTORY_BEAN, destroyMethod = "destroy")
    public DefaultKafkaProducerFactory<String, TBase<?, ?>> thriftProducerFactory(
            @Qualifier(BOOTSTRAP_ADDRESS_BEAN) String bootstrapAddress) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean(KAFKA_TEMPLATE_BEAN)
    public KafkaTemplate<String, TBase<?, ?>> thriftKafkaTemplate(
            @Qualifier(PRODUCER_FACTORY_BEAN)
            DefaultKafkaProducerFactory<String, TBase<?, ?>> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaProducer<TBase<?, ?>> testThriftKafkaProducer(
            @Qualifier(KAFKA_TEMPLATE_BEAN) KafkaTemplate<String, TBase<?, ?>> kafkaTemplate) {
        return new KafkaProducer<>(kafkaTemplate);
    }
}
