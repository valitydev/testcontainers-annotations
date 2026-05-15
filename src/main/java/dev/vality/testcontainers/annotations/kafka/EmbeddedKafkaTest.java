package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.KafkaTestConfig;
import dev.vality.testcontainers.annotations.kafka.config.KafkaConsumer;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @EmbeddedKafkaTest} подключает и запускает embedded Kafka
 * {@link org.springframework.kafka.test.EmbeddedKafkaBroker}, также
 * настройки embedded брокера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация требует дополнительной конфигурации {@link EmbeddedKafkaTest#topics()}
 * <p><h3>Примеры</h3>
 * <p>В примере ниже создается обертка над аннотацией для конкретного приложения с инициализацией
 * конкретных топиков приложения. Эту обертку можно позже переиспользовать для любых тестов,
 * требующих embedded Kafka без запуска Docker контейнера
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @EmbeddedKafkaTest(
 *         properties = {
 *                 "kafka.topics.invoicing.consume.enabled=true",
 *                 "kafka.topics.invoice-template.consume.enabled=true",
 *                 "kafka.state.cache.size=0"},
 *         topics = {
 *                 "magista-invoicing-test",
 *                 "magista-invoice-template-test"})
 * public @interface CustomEmbeddedKafkaTest {
 * }}</pre>
 * <p>В примере ниже {@link EmbeddedKafkaTest} подключается напрямую
 * к {@link SpringBootTest} для проведения теста консьюмера, который читает данные из топика
 * <pre> {@code
 * @EmbeddedKafkaTest(
 *         properties = {
 *                 "kafka.topics.invoicing.id=reporter-invoicing-test",
 *                 "kafka.topics.invoicing.enabled=true"},
 *         topics = "reporter-invoicing-test")
 * @SpringBootTest
 * public class KafkaListenerTest {
 *
 *     @Autowired
 *     private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
 *
 *   ...
 * }}</pre>
 *
 * @see KafkaTestcontainer @KafkaTestcontainer
 * @see KafkaTestcontainerSingleton @KafkaTestcontainerSingleton
 * @see EmbeddedKafka @EmbeddedKafka
 * @see KafkaProducer KafkaProducer
 * @see KafkaConsumer KafkaConsumer
 * @see KafkaTestConfig KafkaTestConfig
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EmbeddedKafka
@ExtendWith(EmbeddedKafkaTestExtension.class)
@KafkaTestConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface EmbeddedKafkaTest {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"kafka.topics.invoicing.consume.enabled=true",...}
     */
    String[] properties() default {};

    /**
     * Обязательный параметр — здесь перечисляются имена топиков, которые требуется создать при старте embedded Kafka
     * <p>
     * пример — topics = {"magista-invoicing-test",...}
     */
    @AliasFor(annotation = EmbeddedKafka.class, attribute = "topics")
    String[] topics() default {};

    /**
     * Очищать топики между тестами
     *
     * @return true - данные между тестами удаляются из embedded Kafka
     */
    boolean cleanupTopics() default true;

    /**
     * Топики, которые не нужно очищать между тестами.
     * Используется только если {@link #cleanupTopics()} = true
     * <p>
     * пример — excludeCleanupTopics = {"magista-invoicing-test"}
     */
    String[] excludeCleanupTopics() default {};
}
