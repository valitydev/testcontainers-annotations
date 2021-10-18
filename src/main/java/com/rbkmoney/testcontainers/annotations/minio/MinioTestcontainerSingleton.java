package com.rbkmoney.testcontainers.annotations.minio;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @MinioTestcontainerSingleton} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.GenericContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p>Пример использования в коде — в
 * <a href="https://github.com/rbkmoney/file-storage/tree/master/src/test/java/com/rbkmoney/file/storage">file-storage</a>
 * <p><h3>Синглтон</h3>
 * <p>Аннотация является {@link MinioTestcontainer} в режиме
 * <a href="https://ru.wikipedia.org/wiki/Одиночка_(шаблон_проектирования)">синглтона</a> —
 * создаваемый тестконтейнер {@link org.testcontainers.containers.GenericContainer}
 * будет создан один раз (в разрезе всего набора тестовых классов в пакете test) и будет переиспользоваться
 * в каждом тестовом классе
 * <p>Аннотация никак не обеспечивает изоляцию данных между тестами. Но попробуйте найти айдишник файла
 * в бакете из другого файла с тестами KEKW
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link MinioTestcontainerSingleton} подключается напрямую
 * к {@link SpringBootTest} для проведения теста, при котором идет запись и чтение данных из хранилища с файлами
 * <pre> {@code
 * @MinioTestcontainerSingleton
 * @SpringBootTest
 * public class FileStorageTest {
 * }}</pre>
 * <p>В примере ниже {@link MinioTestcontainerSingleton} подключается к
 * {@link com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @MinioTestcontainerSingleton
 * @DefaultSpringBootTest
 * public @interface MinioSpringBootITest {
 * }}</pre>
 * <pre> {@code
 * @MinioSpringBootITest
 * public class FileStorageTest {
 * }}</pre>
 *
 * @see MinioTestcontainer @MinioTestcontainer
 * @see ExtendWith @ExtendWith
 * @see org.testcontainers.containers.GenericContainer PostgreSQLContainer
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MinioTestcontainerExtension.class)
public @interface MinioTestcontainerSingleton {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"minio.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * параметр для инициализации s3 client
     * <p>
     * name should be in lowercase!
     */
    String bucketName() default "test";

}
