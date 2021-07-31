# `@MinioTestcontainer`

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
String bucketName() default "test"; // lowercase!!
```

Стандартные настройки для подключения `s3` клиента 

## `@MinioTestcontainerSingleton`

Аннотация является `@MinioTestcontainer` [*в режиме синглтона*](https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)) — создаваемый тестконтейнер `GenericContainer` будет создан *один раз* (в разрезе всего набора тестовых классов в пакете `test`) и будет переиспользоваться в каждом тестовом классе

#### Примеры использования

```java
@MinioTestcontainer // or @MinioTestcontainerSingleton
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileStorageTest {

  ...

```

```java

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@MinioTestcontainer 
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application.yml")
@DirtiesContext
public @interface MinioSpringBootITest {
}

```

Еще пример

![image](https://user-images.githubusercontent.com/19729841/127736286-283416ce-dfc7-4ccc-94df-c4ce0fc5e180.png)
