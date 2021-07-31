# `@CephTestcontainer`

Аннотация подключает и запускает тестконтейнер `GenericContainer` + настройки
контейнера будут проинициализированы в контекст тестового приложения

Аннотация не требует дополнительной конфигурации

Пример использования — в [file-storage](https://github.com/rbkmoney/file-storage/tree/master/src/test/java/com/rbkmoney/file/storage)

## Параметры аннотации

```java
String[] properties() default {};
```

`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kek=true"}`

```java
String signingRegion() default "RU";
String clientProtocol() default "HTTP";
String clientMaxErrorRetry() default "10";
String bucketName() default "TEST";
```

Стандартные настройки для подключения `s3` клиента 

## `@CephTestcontainerSingleton`

Аннотация является `@CephTestcontainer` [*в режиме синглтона*](https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)) — создаваемый тестконтейнер `GenericContainer` будет создан *один раз* (в разрезе всего набора тестовых классов в пакете `test`) и будет переиспользоваться в каждом тестовом классе

#### Примеры использования

```java
@CephTestcontainer // or @CephTestcontainerSingleton
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileStorageTest {

  ...

```

```java

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@CephTestcontainer 
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application.yml")
@DirtiesContext
public @interface CephSpringBootITest {
}

```

Еще пример

![image](https://user-images.githubusercontent.com/19729841/127736179-6fce6001-ea2a-40e7-9d5e-51dea7ff01fc.png)
