# `@ClickhouseTestcontainer`

Аннотация подключает и запускает тестконтейнер `ClickHouseContainer` + настройки
контейнера будут проинициализированы в контекст тестового приложения

Аннотация не требует дополнительной конфигурации

Пример использования — в [fraudbusters](https://github.com/rbkmoney/fraudbusters/pull/137)

## Параметры аннотации

```java
String[] properties() default {};
```

`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kek=true"}`

```java
String[] migrations();
```

`migrations()` **обязательный** параметр — здесь указываются файлы с миграциями для кликхауза, например — `migrations = {"sql/drop_tables.sql","sql/db_init.sql",...}`

Если контейнер переиспользуется в режиме синглтона первой миграцией есть смысл указывать дроп таблиц для изоляции состояний тестов. Для миграции схемы используется `ChInitializer.initAllScripts`, при ошибке процесса приложение не запустится

## `@ClickhouseTestcontainerSingleton`

Аннотация является `@ClickhouseTestcontainer` [*в режиме синглтона*](https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)) — создаваемый тестконтейнер `ClickHouseContainer` будет создан *один раз* (в разрезе всего набора тестовых классов в пакете `test`) и будет переиспользоваться в каждом тестовом классе

###### Дополнительные обертки

`@DefaultSpringBootTest` представляет из себя типичный для домена [rbkmoney](https://github.com/rbkmoney)) набор аннотаций используемых с `SpringBootTest` при тестированию спринговых приложений   

#### Примеры использования

```java
@ClickhouseTestcontainer // or @ClickhouseTestcontainerSingleton
@SpringBootTest // or @DefaultSpringBootTest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```

```java

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ClickhouseTestcontainer
@DefaultSpringBootTest
public @interface ClickhouseSpringBootITest {
}

```

Еще пример

![image](https://user-images.githubusercontent.com/19729841/127735823-3add1f4c-ede0-4bee-b328-458eabaa22ac.png)

