package com.rbkmoney.testcontainers.annotations.clickhouse;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @ClickhouseTestcontainerSingleton} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.ClickHouseContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация требует дополнительной конфигурации
 * {@link ClickhouseTestcontainerSingleton#migrations()}} и {@link ClickhouseTestcontainerSingleton#dbNameShouldBeDropped()}
 * <p>Пример использования в коде — в
 * <a href="https://github.com/rbkmoney/fraudbusters/pull/137">fraudbusters</a>
 * <p><h3>Синглтон</h3>
 * <p>Аннотация является {@link ClickhouseTestcontainer} в режиме
 * <a href="https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)">синглтона</a> —
 * создаваемый тестконтейнер {@link org.testcontainers.containers.ClickHouseContainer}
 * будет создан один раз (в разрезе всего набора тестовых классов в пакете test) и будет переиспользоваться
 * в каждом тестовом классе
 * <p> Аннотация использует {@link ClickhouseTestcontainerSingleton#dbNameShouldBeDropped()} для изоляции тестовых данных
 * в разрезе каждого тестового метода. Перед запуском каждого файла с тестами данные в базе
 * будут стерты посредством выполенение команды 'DROP DATABASE IF EXISTS', при этом контейнер
 * остается висеть запущенным,
 * таким образом, в каждом файле с тестами база будет свежая и чистая
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link ClickhouseTestcontainerSingleton} подключается напрямую
 * к {@link SpringBootTest} для проведения теста DAO слоя, при котором идет запись и чтение данных из базы данных
 * <pre> {@code
 * @ClickhouseTestcontainerSingleton(
 *         dbNameShouldBeDropped = "fraud",
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
 * <p>В примере ниже {@link ClickhouseTestcontainerSingleton} подключается к
 * {@link com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @ClickhouseTestcontainerSingleton(
 *         dbNameShouldBeDropped = "fraud",
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
 * @see ClickhouseTestcontainer @ClickhouseTestcontainer
 * @see ExtendWith @ExtendWith
 * @see org.testcontainers.containers.ClickHouseContainer ClickHouseContainer
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClickhouseTestcontainerExtension.class)
public @interface ClickhouseTestcontainerSingleton {

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

    /**
     * Обязательный параметр — здесь указывается имя базы данных, которая будет дропнута
     * при каждом запуске нового файла с тестами, таким образом обеспечивая изоляцию данных между тестами
     * и предоставляя каждый раз чистую базу
     * <p>
     * пример — dbNameShouldBeDropped = "fraud"
     */
    String dbNameShouldBeDropped();

}
