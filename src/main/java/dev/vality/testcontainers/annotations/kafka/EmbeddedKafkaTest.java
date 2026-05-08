package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.KafkaTestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Starts Spring embedded Kafka and initializes common Spring/Kafka bootstrap properties.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EmbeddedKafka
@ExtendWith(EmbeddedKafkaTestExtension.class)
@KafkaTestConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface EmbeddedKafkaTest {

    /**
     * Alias for {@link EmbeddedKafka#topics()}.
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "topics")
    String[] topics() default {};

    /**
     * Alias for {@link EmbeddedKafka#partitions()}.
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "partitions")
    int partitions() default 1;

    /**
     * Alias for {@link EmbeddedKafka#count()}.
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "count")
    int count() default 1;

    /**
     * Alias for {@link EmbeddedKafka#brokerProperties()}.
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "brokerProperties")
    String[] brokerProperties() default {};

    /**
     * Alias for {@link EmbeddedKafka#kraft()}.
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "kraft")
    boolean kraft() default false;

    /**
     * Аналогичный параметр как у аннотации {@code SpringBootTest#properties()}.
     */
    String[] properties() default {};

    /**
     * Удалять и пересоздавать объявленные топики перед каждым тестом.
     */
    boolean cleanupTopics() default true;

    /**
     * Топики, которые не нужно очищать между тестами.
     */
    String[] excludeCleanupTopics() default {};
}
