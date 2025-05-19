package dev.vality.testcontainers.annotations.postgresql;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @PostgresqlTestcontainer} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.PostgreSQLContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p>Пример использования в коде — в
 * <a href="https://github.com/ValityDev/magista/tree/master/src/test/java/com/ValityDev/magista/config">magista</a>
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link PostgresqlTestcontainer} подключается напрямую
 * к {@link SpringBootTest} для проведения теста DAO слоя, при котором идет запись и чтение данных из базы данных
 * <pre> {@code
 * @PostgresqlTestcontainer
 * @SpringBootTest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *
 *   ...
 * }}</pre>
 *
 * @see PostgresqlTestcontainerSingleton @PostgresqlTestcontainerSingleton
 * @see ExtendWith @ExtendWith
 * @see PostgresqlTestcontainerExtension PostgresqlTestcontainerExtension
 * @see org.testcontainers.containers.PostgreSQLContainer PostgreSQLContainer
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PostgresqlTestcontainerExtension.class)
public @interface PostgresqlTestcontainer {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"postgresql.make.happy=true",...}
     */
    String[] properties() default {};

}