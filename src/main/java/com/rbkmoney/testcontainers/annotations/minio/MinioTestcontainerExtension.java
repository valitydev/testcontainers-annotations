package com.rbkmoney.testcontainers.annotations.minio;

import com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Optional;

import static com.rbkmoney.testcontainers.annotations.minio.MinioTestcontainerFactory.MINIO_PASSWORD;
import static com.rbkmoney.testcontainers.annotations.minio.MinioTestcontainerFactory.MINIO_USER;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * {@code @MinioTestcontainerExtension} инициализирует тестконтейнер из {@link MinioTestcontainerFactory},
 * настраивает, стартует, валидирует и останавливает
 * <p><h3>{@link MinioTestcontainerExtension.MinioTestcontainerContextCustomizerFactory}</h3>
 * <p>Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован
 * под капотом аннотаций, на уровне реализации интерфейса  —
 * информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки
 * инициализируются через {@link TestPropertyValues} и сливаются с текущим получаемым контекстом
 * приложения {@link ConfigurableApplicationContext}
 * <p>Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов
 * в файле META-INF/spring.factories
 *
 * @see MinioTestcontainerFactory MinioTestcontainerFactory
 * @see MinioTestcontainerExtension.MinioTestcontainerContextCustomizerFactory MinioTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class MinioTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<GenericContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = MinioTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var container = MinioTestcontainerFactory.singletonContainer();
            if (!container.isRunning()) {
                GenericContainerUtil.startContainer(container);
            }
            THREAD_CONTAINER.set(container);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<MinioTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), MinioTestcontainer.class);
    }

    private static Optional<MinioTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainer.class);
    }

    private static Optional<MinioTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), MinioTestcontainerSingleton.class);
    }

    private static Optional<MinioTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainerSingleton.class);
    }

    public static class MinioTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findPrototypeAnnotation(testClass).isPresent()) {
                    var annotation = findPrototypeAnnotation(testClass).get(); //NOSONAR
                    init(
                            context,
                            annotation.bucketName(),
                            annotation.properties());
                } else {
                    findSingletonAnnotation(testClass).ifPresent(
                            annotation -> init(
                                    context,
                                    annotation.bucketName(),
                                    annotation.properties()));
                }
            };
        }

        private void init(
                ConfigurableApplicationContext context,
                String bucketName,
                String[] properties) {
            var container = THREAD_CONTAINER.get();
            TestPropertyValues.of(
                    // deprecated
                    "storage.endpoint=" + container.getContainerIpAddress() + ":" +
                            container.getMappedPort(9000),
//                    "storage.signingRegion=" + signingRegion,
                    "storage.accessKey=" + loadDefaultLibraryProperty(MINIO_USER),
                    "storage.secretKey=" + loadDefaultLibraryProperty(MINIO_PASSWORD),
//                    "storage.clientProtocol=" + clientProtocol,
//                    "storage.clientMaxErrorRetry=" + clientMaxErrorRetry,
                    "storage.bucketName=" + bucketName,
                    // --
                    "s3.endpoint=" + container.getContainerIpAddress() + ":" + container.getMappedPort(9000),
                    "s3.bucket-name=" + bucketName,
//                    "s3.signing-region=" + signingRegion,
//                    "s3.client-protocol=" + clientProtocol,
//                    "s3.client-max-error-retry=" + clientMaxErrorRetry,
//                    "s3.signer-override=" + signerOverride,
                    "s3.access-key=" + loadDefaultLibraryProperty(MINIO_USER),
                    "s3.secret-key=" + loadDefaultLibraryProperty(MINIO_PASSWORD),
                    "s3-sdk-v2.enabled=false",
                    "s3-sdk-v2.endpoint=" + String.format("http://%s:%d/", container.getHost(),
                            container.getMappedPort(9000)),
                    "s3-sdk-v2.bucket-name=" + bucketName,
//                    "s3-sdk-v2.region=" + signingRegion,
                    "s3-sdk-v2.access-key=" + loadDefaultLibraryProperty(MINIO_USER),
                    "s3-sdk-v2.secret-key=" + loadDefaultLibraryProperty(MINIO_PASSWORD))
                    .and(properties)
                    .applyTo(context);
        }
    }
}
