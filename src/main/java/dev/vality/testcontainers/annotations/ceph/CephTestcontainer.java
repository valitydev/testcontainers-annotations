package dev.vality.testcontainers.annotations.ceph;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация {@code @CephTestcontainer} подключает и запускает тестконтейнер
 * {@link org.testcontainers.containers.GenericContainer}, также
 * настройки контейнера будут проинициализированы в контекст тестового приложения
 * <p>Аннотация не требует дополнительной конфигурации
 * <p>Пример использования в коде — в
 * <a href="https://github.com/ValityDev/file-storage/tree/master/src/test/java/com/ValityDev/file/storage">file-storage</a>
 * <p><h3>Примеры</h3>
 * <p>В примере ниже {@link CephTestcontainer} подключается напрямую
 * к {@link SpringBootTest} для проведения теста, при котором идет запись и чтение данных из хранилища с файлами
 * <pre> {@code
 * @CephTestcontainer
 * @SpringBootTest
 * public class FileStorageTest {
 * }}</pre>
 *
 * @see CephTestcontainerSingleton @CephTestcontainerSingleton
 * @see ExtendWith @ExtendWith
 * @see CephTestcontainerExtension CephTestcontainerExtension
 * @see org.testcontainers.containers.GenericContainer GenericContainer
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CephTestcontainerExtension.class)
public @interface CephTestcontainer {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"ceph.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * параметр для инициализации s3 client
     */
    String bucketName() default "TEST";

}
