# `@KafkaTestcontainer`

Аннотация подключает и запускает тестконтейнер `KafkaContainer` + настройки
контейнера будут проинициализированы в контекст тестового приложения

Аннотация требует дополнительной конфигурации (см. ниже)

Пример использования `@KafkaTestcontainer` с `KafkaProducer` — в [magista](https://github.com/rbkmoney/magista/blob/master/src/test/java/com/rbkmoney/magista/kafka/InvoicingListenerTest.java)

Пример использования `@KafkaTestcontainer` с `KafkaConsumer` — в [sink-drinker](https://github.com/rbkmoney/sink-drinker/blob/master/src/test/java/com/rbkmoney/sinkdrinker/kafka/KafkaSenderTest.java)

## Параметры аннотации

```java
String[] properties() default {};
```

`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kafka.topics.invoicing.consume.enabled=true",...}`

```java
String[] topicsKeys();
```

`topicsKeys()` **обязательный** параметр — здесь перечисляются параметры, которые хранят в себе имена топиков, которые требуется создать при старте кафки, например — `topicsKeys = {"kafka.topics.invoicing.id",...}`

Создание топиков происходит через `AdminClient`, также есть дополнительная валидация результатов создания топиков, без валидации приложение не запустится 

## `@KafkaTestcontainerSingleton`

Аннотация является `@KafkaTestcontainer` [*в режиме синглтона*](https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)) — создаваемый тестконтейнер `KafkaContainer` будет создан *один раз* (в разрезе всего набора тестовых классов в пакете `test`) и будет переиспользоваться в каждом тестовом классе

###### Дополнительные обертки

`@DefaultSpringBootTest` представляет из себя типичный для домена [rbkmoney](https://github.com/rbkmoney) набор аннотаций используемых с `SpringBootTest` при тестированию спринговых приложений 

`@KafkaProducerSpringBootTest`, `@KafkaConsumerSpringBootTest` набор аннотаций используемых с `SpringBootTest` при тестированию спринговых приложений c кафкой

## Init `@ContextConfiguration`

`KafkaConsumerConfig`, `KafkaProducerConfig` — кофиги для инциализации тестового консьюмера или продьюссера в зависимости от того, что проверяется в тесте

При использовании `KafkaProducerConfig` в приложении можно использовать продьюссер для отправки данных в брокер

```java

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;
    
    ...
    
    testThriftKafkaProducer.send(invoicingTopicName, sinkEvent);
    
    ...
```

Пример использования `KafkaProducer` — в [магисте](https://github.com/rbkmoney/magista/blob/master/src/test/java/com/rbkmoney/magista/kafka/InvoicingListenerTest.java)

При использовании `KafkaConsumerConfig` в приложении можно использовать консьюмер для получения данных из брокера

```java

    @Autowired
    private KafkaConsumer<Event> testPayoutEventKafkaConsumer;
    
    ...
    
    testPayoutEventKafkaConsumer.read(topicName, data -> readEvents.add(data.value()));
    Unreliables.retryUntilTrue(TIMEOUT, TimeUnit.SECONDS, () -> readEvents.size() == expected);
    
    ...
```

Пример использования `KafkaConsumer` — в [sink-drinker](https://github.com/rbkmoney/sink-drinker/blob/master/src/test/java/com/rbkmoney/sinkdrinker/kafka/KafkaSenderTest.java)

#### Примеры использования

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaTestcontainer(
        properties = {
                "kafka.topics.invoicing.consume.enabled=true",
                "kafka.topics.invoice-template.consume.enabled=true",
                "kafka.topics.pm-events-payout.consume.enabled=true",
                "kafka.state.cache.size=0"},
        topicsKeys = {
                "kafka.topics.invoicing.id",
                "kafka.topics.invoice-template.id",
                "kafka.topics.pm-events-payout.id"})
public @interface ApplicationKafkaTestcontainer {
}

```

`@KafkaTestcontainer` имплементируется с настройками кафки для приложения

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainer
@ApplicationKafkaTestcontainer
@KafkaProducerSpringBootTest
public @interface ApplicationSpringBootITest {
}
```

Создается обертка в виде аннотации, которая при использовании по очереди поднимает базу, кафку (нашу имплементированную
аннотацию) и `SpringBootTest` для запуска спрингового контекста

Еще пример использования 

![image](https://user-images.githubusercontent.com/19729841/127735631-ef069f6c-9707-452e-ae03-a389d64adbbe.png)
