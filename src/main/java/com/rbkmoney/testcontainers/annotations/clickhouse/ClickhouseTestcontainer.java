package com.rbkmoney.testcontainers.annotations.clickhouse;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @ClickhouseTestcontainer} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.ClickHouseContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация требует дополнительной конфигурации {@link ClickhouseTestcontainer#migrations()}
 * <p>Пример использования в коде — в
 * <a href="https://github.com/rbkmoney/fraudbusters/pull/137">fraudbusters</a>
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link ClickhouseTestcontainer} подключается напрямую
 * к {@link SpringBootTest} для проведения теста DAO слоя, при котором идет запись и чтение данных из базы данных
 * <pre> {@code
 * @ClickhouseTestcontainer(
 *         migrations = {
 *                 "sql/db_init.sql",
 *                 "sql/V4__create_payment.sql"})
 * @SpringBootTest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *
 *   ...
 * }}</pre>
 * <p>В примере ниже {@link ClickhouseTestcontainer} подключается к
 * {@link com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @ClickhouseTestcontainer(
 *         migrations = {
 *                 "sql/db_init.sql",
 *                 "sql/V4__create_payment.sql"})
 * @DefaultSpringBootTest
 * public @interface ClickhouseSpringBootITest {
 *
 * }}</pre>
 * <pre> {@code
 * @ClickhouseSpringBootITest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *     ...
 * }}</pre>
 *
 * @see ClickhouseTestcontainerSingleton @ClickhouseTestcontainerSingleton
 * @see ExtendWith @ExtendWith
 * @see ClickhouseTestcontainerExtension ClickhouseTestcontainerExtension
 * @see org.testcontainers.containers.ClickHouseContainer ClickHouseContainer
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClickhouseTestcontainerExtension.class)
public @interface ClickhouseTestcontainer {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"сlickhouse.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * Обязательный параметр — здесь указываются файлы с миграциями для кликхауза
     * <p>
     * пример — migrations = {"sql/db_init.sql","sql/V1__create_payment.sql",...}
     */
    String[] migrations();

}
