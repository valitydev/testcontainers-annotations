package com.rbkmoney.testcontainers.annotations.clickhouse;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClickhouseTestcontainerExtension.class)
public @interface ClickhouseTestcontainer {

    /**
     * properties = {"clickhouse.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * migrations = {"sql/db_init.sql","sql/V1__create_payment.sql",...}
     */
    String[] migrations();

}
