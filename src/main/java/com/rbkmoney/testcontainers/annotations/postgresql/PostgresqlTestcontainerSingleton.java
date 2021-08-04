package com.rbkmoney.testcontainers.annotations.postgresql;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @PostgresqlTestcontainerSingleton} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.PostgreSQLContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p>Пример использования в коде — в
 * <a href="https://github.com/rbkmoney/magista/tree/master/src/test/java/com/rbkmoney/magista/config">magista</a>
 * <p><h3>Синглтон</h3>
 * <p>Аннотация является {@link PostgresqlTestcontainer} в режиме
 * <a href="https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)">синглтона</a> —
 * создаваемый тестконтейнер {@link org.testcontainers.containers.PostgreSQLContainer}
 * будет создан один раз (в разрезе всего набора тестовых классов в пакете test) и будет переиспользоваться
 * в каждом тестовом классе
 * <p> Аннотация использует {@link Transactional} для изоляции тестовых данных
 * в разрезе каждого тестового метода. После каждого теста аннотация будет делать роллбек всех изменений,
 * таким образом, в каждом тесте база будет свежая и чистая
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link PostgresqlTestcontainerSingleton} подключается напрямую
 * к {@link SpringBootTest} для проведения теста DAO слоя, при котором идет запись и чтение данных из базы данных
 * <pre> {@code
 * @PostgresqlTestcontainerSingleton
 * @SpringBootTest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *
 *   ...
 * }}</pre>
 * <p>В примере ниже {@link PostgresqlTestcontainerSingleton} подключается к
 * {@link com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @PostgresqlTestcontainerSingleton
 * @DefaultSpringBootTest
 * public @interface PostgresqlSpringBootITest {
 *
 * }}</pre>
 * <pre> {@code
 * @PostgresqlSpringBootITest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *     ...
 * }}</pre>
 *
 * @see PostgresqlTestcontainer @PostgresqlTestcontainer
 * @see ExtendWith @ExtendWith
 * @see Transactional @Transactional
 * @see org.testcontainers.containers.PostgreSQLContainer PostgreSQLContainer
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PostgresqlTestcontainerExtension.class)
@Transactional
public @interface PostgresqlTestcontainerSingleton {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"postgresql.make.happy=true",...}
     */
    String[] properties() default {};

}
