# `@PostgresqlTestcontainer`

Аннотация подключает и запускает тестконтейнер `PostgreSQLContainer` + настройки
контейнера будут проинициализированы в контекст тестового приложения

Аннотация не требует дополнительной конфигурации

Пример использования — в [magista](https://github.com/rbkmoney/magista/tree/master/src/test/java/com/rbkmoney/magista/config)

## Параметры аннотации

```java
String[] properties() default {};
```

`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kek=true"}`

## `@PostgresqlTestcontainerSingleton`

Аннотация является `@PostgresqlTestcontainer` [*в режиме синглтона*](https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)) — создаваемый тестконтейнер `PostgreSQLContainer` будет создан *один раз* (в разрезе всего набора тестовых классов в пакете `test`) и будет переиспользоваться в каждом тестовом классе

###### Дополнительные обертки

`@DefaultSpringBootTest` представляет из себя типичный для домена [rbkmoney](https://github.com/rbkmoney)) набор аннотаций используемых с `SpringBootTest` при тестированию спринговых приложений   

#### Примеры использования

```java
@PostgresqlTestcontainer // or @PostgresqlTestcontainerSingleton
@SpringBootTest // or @DefaultSpringBootTest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```

```java

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainerSingleton
@DefaultSpringBootTest
public @interface PostgresqlSpringBootITest {
}

```
