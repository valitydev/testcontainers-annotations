package dev.vality.testcontainers.annotations.postgresql;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Starts an embedded PostgreSQL process and initializes Spring datasource properties.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EmbeddedPostgresqlTestExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface EmbeddedPostgresqlTest {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}.
     */
    String[] properties() default {};

    /**
     * Embedded PostgreSQL database name.
     */
    String database() default "postgres";

    /**
     * Embedded PostgreSQL user.
     */
    String username() default "postgres";

    /**
     * Embedded PostgreSQL password. Zonky embedded PostgreSQL uses an empty password by default.
     */
    String password() default "";

    /**
     * Очищать таблицы между тестами.
     */
    boolean truncateTables() default true;

    /**
     * Таблицы, которые не нужно очищать между тестами.
     */
    String[] excludeTruncateTables() default {};
}
