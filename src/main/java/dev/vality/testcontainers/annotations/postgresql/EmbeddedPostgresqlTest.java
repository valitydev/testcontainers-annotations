package dev.vality.testcontainers.annotations.postgresql;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @EmbeddedPostgresqlTest} подключает и запускает embedded PostgreSQL
 * {@link io.zonky.test.db.postgres.embedded.EmbeddedPostgres}, также
 * настройки embedded базы данных будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p><h3>Примеры</h3>
 * <p>В примере ниже создается обертка над аннотацией для конкретного приложения с инициализацией
 * дополнительных Spring properties. Эту обертку можно позже переиспользовать для любых тестов,
 * требующих embedded PostgreSQL без запуска Docker контейнера
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @EmbeddedPostgresqlTest(
 *         properties = {
 *                 "spring.flyway.schemas=public",
 *                 "spring.jpa.hibernate.ddl-auto=none"})
 * public @interface CustomEmbeddedPostgresqlTest {
 * }}</pre>
 * <p>В примере ниже {@link EmbeddedPostgresqlTest} подключается напрямую
 * к {@link SpringBootTest} для проведения теста DAO слоя, при котором идет запись и чтение данных из базы данных
 * <pre> {@code
 * @EmbeddedPostgresqlTest
 * @SpringBootTest
 * public class AdjustmentDaoTest {
 *
 *     @Autowired
 *     private AdjustmentDao adjustmentDao;
 *
 *   ...
 * }}</pre>
 *
 * @see PostgresqlTestcontainer @PostgresqlTestcontainer
 * @see PostgresqlTestcontainerSingleton @PostgresqlTestcontainerSingleton
 * @see EmbeddedPostgresqlTestExtension EmbeddedPostgresqlTestExtension
 * @see io.zonky.test.db.postgres.embedded.EmbeddedPostgres EmbeddedPostgres
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EmbeddedPostgresqlTestExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface EmbeddedPostgresqlTest {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"spring.flyway.schemas=public",...}
     */
    String[] properties() default {};

    /**
     * Имя embedded PostgreSQL базы данных
     * <p>
     * пример — database = "postgres"
     */
    String database() default "postgres";

    /**
     * Имя embedded PostgreSQL пользователя
     * <p>
     * пример — username = "postgres"
     */
    String username() default "postgres";

    /**
     * Пароль embedded PostgreSQL пользователя.
     * Zonky embedded PostgreSQL по умолчанию использует пустой пароль
     * <p>
     * пример — password = ""
     */
    String password() default "";

    /**
     * Очищать таблицы между тестами
     *
     * @return true - данные между тестами удаляются из embedded PostgreSQL
     */
    boolean truncateTables() default true;

    /**
     * Таблицы, которые не нужно очищать между тестами.
     * Используется только если {@link #truncateTables()} = true
     * <p>
     * пример — excludeTruncateTables = {"schema_history", "reference_data"}
     */
    String[] excludeTruncateTables() default {};
}
