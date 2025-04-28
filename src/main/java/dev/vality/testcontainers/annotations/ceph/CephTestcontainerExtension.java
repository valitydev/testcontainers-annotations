package dev.vality.testcontainers.annotations.ceph;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader;
import lombok.extern.slf4j.Slf4j;
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

import static dev.vality.testcontainers.annotations.ceph.CephTestcontainerFactory.ACCESS_KEY;
import static dev.vality.testcontainers.annotations.ceph.CephTestcontainerFactory.SECRET_KEY;

/**
 * {@code @CephTestcontainerExtension} инициализирует тестконтейнер из {@link CephTestcontainerFactory},
 * настраивает, стартует, валидирует и останавливает
 * <p><h3>{@link CephTestcontainerExtension.CephTestcontainerContextCustomizerFactory}</h3>
 * <p>Инициализация настроек контейнеров в спринговый контекст тестового приложения реализован
 * под капотом аннотаций, на уровне реализации интерфейса  —
 * информация о настройках используемого тестконтейнера и передаваемые через параметры аннотации настройки
 * инициализируются через {@link TestPropertyValues} и сливаются с текущим получаемым контекстом
 * приложения {@link ConfigurableApplicationContext}
 * <p>Инициализация кастомизированных фабрик с инициализацией настроек осуществляется через описание бинов
 * в файле META-INF/spring.factories
 *
 * @see CephTestcontainerFactory CephTestcontainerFactory
 * @see CephTestcontainerExtension.CephTestcontainerContextCustomizerFactory CephTestcontainerContextCustomizerFactory
 * @see TestPropertyValues TestPropertyValues
 * @see ConfigurableApplicationContext ConfigurableApplicationContext
 * @see BeforeAllCallback BeforeAllCallback
 * @see AfterAllCallback AfterAllCallback
 */
@Slf4j
public class CephTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<GenericContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = CephTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var container = CephTestcontainerFactory.singletonContainer();
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

    private static Optional<CephTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), CephTestcontainer.class);
    }

    private static Optional<CephTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, CephTestcontainer.class);
    }

    private static Optional<CephTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), CephTestcontainerSingleton.class);
    }

    private static Optional<CephTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, CephTestcontainerSingleton.class);
    }

    public static class CephTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

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
                            "storage.endpoint=" + container.getHost() + ":" +
                            container.getMappedPort(8080),
//                    "storage.signingRegion=" + signingRegion,
                    "storage.accessKey=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(ACCESS_KEY),
                    "storage.secretKey=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(SECRET_KEY),
//                    "storage.clientProtocol=" + clientProtocol,
//                    "storage.clientMaxErrorRetry=" + clientMaxErrorRetry,
                    "storage.bucketName=" + bucketName,
                    // --
                            "s3.endpoint=" + container.getHost() + ":" + container.getMappedPort(8080),
                    "s3.bucket-name=" + bucketName,
//                    "s3.signing-region=" + signingRegion,
//                    "s3.client-protocol=" + clientProtocol,
//                    "s3.client-max-error-retry=" + clientMaxErrorRetry,
//                    "s3.signer-override=" + signerOverride,
                    "s3.access-key=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(ACCESS_KEY),
                    "s3.secret-key=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(SECRET_KEY),
                    "s3-sdk-v2.enabled=false",
                    "s3-sdk-v2.endpoint=" + String.format("http://%s:%d/", container.getHost(),
                            container.getMappedPort(8080)),
                    "s3-sdk-v2.bucket-name=" + bucketName,
//                    "s3-sdk-v2.region=" + signingRegion,
                    "s3-sdk-v2.access-key=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(ACCESS_KEY),
                    "s3-sdk-v2.secret-key=" + SpringApplicationPropertiesLoader.loadDefaultLibraryProperty(SECRET_KEY))
                    .and(properties)
                    .applyTo(context);
        }
    }
}
