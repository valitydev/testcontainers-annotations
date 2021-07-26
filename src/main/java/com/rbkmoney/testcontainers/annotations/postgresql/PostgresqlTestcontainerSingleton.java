package com.rbkmoney.testcontainers.annotations.postgresql;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainer(instanceMode = PostgresqlTestcontainer.InstanceMode.SINGLETON)
@Transactional
public @interface PostgresqlTestcontainerSingleton {
}
