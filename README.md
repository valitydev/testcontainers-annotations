# testcontainers-annotations

Библиотека с аннотациями, которые позволяют безшовно подключить внешние докер-контейнеры к интеграционным SpringBoot тестам  
Аннотации являются обертками над [TestContainers](https://github.com/testcontainers/testcontainers-java)


Далее речь идет только о кейсах, когда есть необходимость в интеграционных тестах с внешними зависимостями

----

## Аннотации

На данный момент для библиотеки реализована поддержка 2 контейнеров — `postgres`, `confluentinc/cp-kafka`

Базовые аннотации для использования:

```java
@PostgresqlTestcontainer
```

```java
@KafkaTestcontainer
```

Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован под капотом аннотаций

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">Детали</em>&nbsp;&nbsp;</a>    
</summary>
  
<p>
  
Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован под капотом аннотаций, на уровне реализации интерфейса `ContextCustomizerFactory` — информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки инициализируются через `TestPropertyValues` и сливаются с текущим получаемым контекстом приложения `ConfigurableApplicationContext`
Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов в файле `spring.factories`
  
</p>
  
</details> 

### `@PostgresqlTestcontainer`

Аннотация не требует дополнительной конфигурации, при ее использовании будет поднять тестконтейнер с базой + настройки контейнера будут проинициализированы в контекст тестового приложения

#### Параметры аннотации

```java
InstanceMode instanceMode() default InstanceMode.DEFAULT;
```
`instanceMode()` описывает жизненный цикл аннотации — аннотация может быть использована в рамках одного тестового класса (`InstanceMode.DEFAULT`), который ее использует, либо использована в рамках всего набора тестовых классов в пакете `test` (`InstanceMode.SINGLETON`) — здесь будет создаваться синглтон, при котором каждый следующий тест переиспользует уже созданный тестконтейнер с базой


```java
String[] properties() default {};
```
`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kek=true"}`

#### Дополнительные обертки

`@PostgresqlTestcontainerSingleton` — `@PostgresqlTestcontainer` в режиме `InstanceMode.SINGLETON`  
`@WithPostgresqlSpringBootITest` — обертка для запуска спрингового теста с использованием тестконтейнера с базой. На борту — `@PostgresqlTestcontainer` и `@DefaultSpringBootTest` (представляет из себя дефолтную обертку над `SpringBootTest` типичную для домена [rbkmoney](https://github.com/rbkmoney))  
`@WithPostgresqlSingletonSpringBootITest` — аналог `@WithPostgresqlSpringBootITest` только с `@PostgresqlTestcontainerSingleton`  

#### Примеры использования

```java
@PostgresqlTestcontainer
@SpringBootTest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```
```java
@PostgresqlTestcontainerSingleton
@DefaultSpringBootTest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```
```java
@WithPostgresqlSpringBootITest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```

### `@KafkaTestcontainer`

При ее использовании будет поднять тестконтейнер с кафкой + настройки контейнера будут проинициализированы в контекст тестового приложения  
Аннотация требует дополнительной конфигурации (см. ниже)

#### Параметры аннотации

```java
InstanceMode instanceMode() default InstanceMode.DEFAULT;
```
`instanceMode()` описывает жизненный цикл аннотации — аннотация может быть использована в рамках одного тестового класса (`InstanceMode.DEFAULT`), который ее использует, либо использована в рамках всего набора тестовых классов в пакете `test` (`InstanceMode.SINGLETON`) — здесь будет создаваться синглтон, при котором каждый следующий тест переиспользует уже созданный тестконтейнер с кафкой

```java
String[] properties() default {};
```
`properties()` аналогичный параметр как у аннотации `SpringBootTest`, например — `properties = {"kafka.topics.invoicing.consume.enabled=true"}`

```java
String[] topicsKeys();
```
`topicsKeys()` при использовании аннотации данный параметр **обязателен** для конфигурации — здесь перечисляются ключи пропертей, которые являются параметрами с названиями топиков, которые требуется создать при старте кафки (создание топиков происходит через `AdminClient`, также есть дополнительная валидация результатов создания топиков, генерируется исключение в случае фейла процесса), например — `topicsKeys = {"kafka.topics.invoicing.id"}`

#### Дополнительные обертки

Здесь дополнительные обертки не реализованы в силу обязательной предконфигурации базовой аннотации с параметрами конкретного приложения

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">Демо обертки</em>&nbsp;&nbsp;</a>    
</summary>
  
<p>

  Хоть возможности создать набор оберток нет, но есть примеры, как это может выглядеть, находятся внутри пакета `com.rbkmoney.testcontainers.annotations.kafka.demo` 
  
  `@DemoKafkaTestcontainer` — пример имплементации `@KafkaTestcontainer`  
  `@DemoKafkaTestcontainerSingleton` — пример имплементации `@KafkaTestcontainer` в режиме `InstanceMode.SINGLETON`  
  `@DemoWithKafkaSpringBootITest` — обертка для запуска спрингового теста с использованием тестконтейнера с кафкой. На борту — `@DemoKafkaTestcontainer` и `@KafkaProducerSpringBootTest` (представляет из себя обертку над `SpringBootTest` типичную для домена [rbkmoney](https://github.com/rbkmoney) c `KafkaProducerConfig` который представляет инструменты для тестирования потребителей)  
  `@DemoWithKafkaSingletonSpringBootITest` — аналог `@DemoWithKafkaSpringBootITest` только с `@DemoKafkaTestcontainerSingleton`  

</p>
  
</details>

#### Примеры использования

Как это используется в магисте:

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaTestcontainer(
        instanceMode = KafkaTestcontainer.InstanceMode.SINGLETON,
        properties = {
                "kafka.topics.invoicing.consume.enabled=true",
                "kafka.topics.invoice-template.consume.enabled=true",
                "kafka.topics.pm-events-payout.consume.enabled=true",
                "kafka.state.cache.size=0"},
        topicsKeys = {
                "kafka.topics.invoicing.id",
                "kafka.topics.invoice-template.id",
                "kafka.topics.pm-events-payout.id"})
public @interface MagistaKafkaTestcontainerSingleton {
}

```

`@KafkaTestcontainer` имплементируется с параметрами магисты для кафки (используется синглтон для реиспользование текущего тестконтейнера и понижения времени прогона тестов на `CI`)

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainerSingleton
@MagistaKafkaTestcontainerSingleton
@KafkaProducerSpringBootTest
public @interface MagistaSpringBootITest {
}
```
Создается обертка в виде аннотации, которая при использовании по очереди поднимает базу, кафку (нашу имплементированную аннотацию) и `SpringBootTest` для запуска спрингового контекста (`@KafkaProducerSpringBootTest` представляет из себя обертку над `SpringBootTest` типичную для домена [rbkmoney](https://github.com/rbkmoney) c `KafkaProducerConfig` который представляет инструменты для тестирования потребителей)


### Ресерч
##### Было

В домене [rbkmoney](https://github.com/rbkmoney) распрострена практика создания интеграционных тестов с использованием цепочки наследования классов, когда родитель является классом с конфигом теста, в которой спрятана вся техническая инициализация спрингового приложения и внешних зависимостей, которые по стандарту являются [TestContainers](https://github.com/testcontainers/testcontainers-java)  
&nbsp;  

Класс-родитель с конфигом для тестов, для которых является необходимым использования `PostgreSQL` в качестве внешней зависимости:

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">AbstractPostgreTestContainerConfig.java</em>&nbsp;&nbsp;</a>    
</summary>
  
<p>

```java
@SpringBootTest
@Testcontainers
@DirtiesContext
@ContextConfiguration(classes = Application.class,
        initializers = Initializer.class)
public abstract class AbstractPostgreTestContainerConfig {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String POSTGRESQL_VERSION = "9.6";

    @Container
    public static final PostgreSQLContainer DB = new PostgreSQLContainer(DockerImageName
            .parse(POSTGRESQL_IMAGE_NAME)
            .withTag(POSTGRESQL_VERSION));

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + DB.getJdbcUrl(),
                    "spring.datasource.username=" + DB.getUsername(),
                    "spring.datasource.password=" + DB.getPassword(),
                    "flyway.url=" + DB.getJdbcUrl(),
                    "flyway.user=" + DB.getUsername(),
                    "flyway.password=" + DB.getPassword()
            ).applyTo(configurableApplicationContext);
        }
    }

}
  
```
  
_К плюсам данного решения можно отнести тот факт, что сами тесты становятся более читаемым, в которых нет ничего лишнего, кроме покрытия бизнес-логики приложения_ 
  
</p>
  
</details> 

Тогда типичный тест `Dao` слоя будет выглядеть как:

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">PaymentDaoTest.java</em>&nbsp;&nbsp;</a>    
</summary>
  
<p>

```java
class PaymentDaoTest extends AbstractPostgreTestContainerConfig {

    @Autowired
    PaymentDao paymentDao;
  
  ...

}

```
</p>
  
</details> 

В этом моменте было желание избавиться от самого способо организации инициализации тестов с использованием порождающего класса, которая влечет повышение запутанности кода, но при этом сохранить приемлемый уровень лаконичности и простоты, свести запутанность к минимому, избавиться от наследования  

##### Стало
При использовании `testcontainers-annotations` для подключения внешней зависимости в файл с тестом необходимо добавить требуемую аннотацию и задать нужный для теста конфиг `SpringBootTest` 

Типичный тест `Dao` слоя, для которого является необходимым использования `PostgreSQL` в качестве внешней зависимости, будет выглядеть как тест, для вызова которого требуется только `@PostgresqlTestcontainer` и `@SpringBootTest`:

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">AdjustmentDaoTest.java</em>&nbsp;&nbsp;</a>    
</summary>
  
<p>

```java
@PostgresqlTestcontainer
@SpringBootTest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```
</p>
  
</details> 

Либо воспользоваться готовой оберткой из библиотеки `@WithPostgresqlSingletonSpringBootITest`:

<details>
  
<summary>
  <a class="btnfire small stroke"><em class="fas fa-chevron-circle-down">AdjustmentDaoTest.java</em>&nbsp;&nbsp;</a>    
</summary>
<p>

```java
@WithPostgresqlSingletonSpringBootITest
public class AdjustmentDaoTest {

    @Autowired
    private AdjustmentDao adjustmentDao;

  ...

```
</p>
  
</details> 

