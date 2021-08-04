package com.rbkmoney.testcontainers.annotations.kafka;

import com.rbkmoney.testcontainers.annotations.postgresql.PostgresqlTestcontainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @KafkaTestcontainer} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.KafkaContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация требует дополнительной конфигурации {@link KafkaTestcontainer#topicsKeys()}
 * <p>Пример использования {@link KafkaTestcontainer} с {@link com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducer} — в
 * <a href="https://github.com/rbkmoney/magista/tree/master/src/test/java/com/rbkmoney/magista/config">magista</a>
 * <p>Пример использования {@link KafkaTestcontainer} с {@link com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumer} — в
 * <a href="https://github.com/rbkmoney/sink-drinker/blob/master/src/test/java/com/rbkmoney/sinkdrinker/kafka/KafkaSenderTest.java">sink-drinker</a>
 * <p><h3>Примеры</h3>
 * <p>В примере ниже создается обертка над аннотацией для конкретного приложения с инициализацией
 * конкретных топиков приложения. Эту обертку можно позже переиспользовать для любых тестов,
 * требующих внешний контейнер с кафкой в разрезе конкретного приложения
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @KafkaTestcontainer(
 *         properties = {
 *                 "kafka.topics.invoicing.consume.enabled=true",
 *                 "kafka.topics.invoice-template.consume.enabled=true",
 *                 "kafka.topics.pm-events-payout.consume.enabled=true",
 *                 "kafka.state.cache.size=0"},
 *         topicsKeys = {
 *                 "kafka.topics.invoicing.id",
 *                 "kafka.topics.invoice-template.id",
 *                 "kafka.topics.pm-events-payout.id"})
 * public @interface CustomKafkaTestcontainer {
 * }}</pre>
 * <p>В примере ниже {@link KafkaTestcontainer} подключается напрямую
 * к {@link PostgresqlTestcontainer} и {@link SpringBootTest}
 * для проведения теста консьюмера, который читает данные из топика
 * <pre> {@code
 * @PostgresqlTestcontainer
 * @KafkaTestcontainer(
 *         properties = "kafka.topic.pm-events-payout.produce.enabled=true",
 *         topicsKeys = "kafka.topic.pm-events-payout.name")
 * @SpringBootTest
 * public class KafkaSenderTest {
 *
 *     @Autowired
 *     private KafkaConsumer<Event> testPayoutEventKafkaConsumer;
 *
 *   ...
 * }}</pre>
 * <p>В примере ниже {@link KafkaTestcontainer} подключается к
 * {@link PostgresqlTestcontainer} и {@link com.rbkmoney.testcontainers.annotations.KafkaSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @PostgresqlTestcontainer
 * @KafkaTestcontainer(
 *         properties = {
 *                 "kafka.topics.invoicing.consume.enabled=true",
 *                 "kafka.topics.invoice-template.consume.enabled=true",
 *                 "kafka.topics.pm-events-payout.consume.enabled=true",
 *                 "kafka.state.cache.size=0"},
 *         topicsKeys = {
 *                 "kafka.topics.invoicing.id",
 *                 "kafka.topics.invoice-template.id",
 *                 "kafka.topics.pm-events-payout.id"})
 * @KafkaSpringBootTest
 * public @interface KafkaPostgresqlSpringBootITest {
 * }}</pre>
 * <pre> {@code
 * @KafkaPostgresqlSpringBootITest
 * public class KafkaListenerTest {
 *
 *     @Autowired
 *     private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
 *     ...
 * }}</pre>
 *
 * @see KafkaTestcontainerSingleton @KafkaTestcontainerSingleton
 * @see ExtendWith @ExtendWith
 * @see KafkaTestcontainerExtension KafkaTestcontainerExtension
 * @see com.rbkmoney.testcontainers.annotations.kafka.config.KafkaProducer KafkaProducer
 * @see com.rbkmoney.testcontainers.annotations.kafka.config.KafkaConsumer KafkaConsumer
 * @see com.rbkmoney.testcontainers.annotations.KafkaSpringBootTest @KafkaSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(KafkaTestcontainerExtension.class)
public @interface KafkaTestcontainer {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"kafka.topics.invoicing.consume.enabled=true",...}
     */
    String[] properties() default {};

    /**
     * Обязательный параметр — здесь перечисляются параметры, которые хранят в себе
     * имена топиков, которые требуется создать при старте кафки
     * Создание топиков происходит через {@link org.apache.kafka.clients.admin.AdminClient},
     * также есть дополнительная валидация результатов создания топиков,
     * без валидации приложение не запустится
     * <p>
     * пример — topicsKeys = {"kafka.topics.invoicing.id",...}
     */
    String[] topicsKeys();

}
