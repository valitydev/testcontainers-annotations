package com.rbkmoney.testcontainers.annotations.minio;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @MinioTestcontainer} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.GenericContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p>Пример использования в коде — в
 * <a href="https://github.com/rbkmoney/file-storage/tree/master/src/test/java/com/rbkmoney/file/storage">file-storage</a>
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link MinioTestcontainer} подключается напрямую
 * к {@link SpringBootTest} для проведения теста, при котором идет запись и чтение данных из хранилища с файлами
 * <pre> {@code
 * @MinioTestcontainer
 * @SpringBootTest
 * public class FileStorageTest {
 * }}</pre>
 * <p>В примере ниже {@link MinioTestcontainer} подключается к
 * {@link com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest},
 * таким образом создается удобная обертка, которую можно использовать для набора тестов
 * <pre> {@code
 * @Target({ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @MinioTestcontainer
 * @DefaultSpringBootTest
 * public @interface MinioSpringBootITest {
 * }}</pre>
 * <pre> {@code
 * @MinioSpringBootITest
 * public class FileStorageTest {
 * }}</pre>
 *
 * @see MinioTestcontainerSingleton @MinioTestcontainerSingleton
 * @see ExtendWith @ExtendWith
 * @see MinioTestcontainerExtension MinioTestcontainerExtension
 * @see org.testcontainers.containers.GenericContainer GenericContainer
 * @see com.rbkmoney.testcontainers.annotations.DefaultSpringBootTest @DefaultSpringBootTest
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MinioTestcontainerExtension.class)
public @interface MinioTestcontainer {

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
