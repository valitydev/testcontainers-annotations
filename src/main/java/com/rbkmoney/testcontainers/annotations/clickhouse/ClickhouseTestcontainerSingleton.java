package com.rbkmoney.testcontainers.annotations.clickhouse;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClickhouseTestcontainerExtension.class)
@Transactional
public @interface ClickhouseTestcontainerSingleton {

    /**
     * properties = {"clickhouse.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * migrations = {"sql/db_init.sql","sql/V1__create_payment.sql",...}
     */
    String[] migrations();

}
