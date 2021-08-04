package com.rbkmoney.testcontainers.annotations.ceph;

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

import static com.rbkmoney.testcontainers.annotations.ceph.CephTestcontainerFactory.ACCESS_KEY;
import static com.rbkmoney.testcontainers.annotations.ceph.CephTestcontainerFactory.SECRET_KEY;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

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
                            annotation.signingRegion(),
                            annotation.clientProtocol(),
                            annotation.clientMaxErrorRetry(),
                            annotation.bucketName(),
                            annotation.properties());
                } else {
                    findSingletonAnnotation(testClass).ifPresent(
                            annotation -> init(
                                    context,
                                    annotation.signingRegion(),
                                    annotation.clientProtocol(),
                                    annotation.clientMaxErrorRetry(),
                                    annotation.bucketName(),
                                    annotation.properties()));
                }
            };
        }

        private void init(
                ConfigurableApplicationContext context,
                String signingRegion,
                String clientProtocol,
                String clientMaxErrorRetry,
                String bucketName,
                String[] properties) {
            var container = THREAD_CONTAINER.get();
            TestPropertyValues.of(
                    "storage.endpoint=" + container.getContainerIpAddress() + ":" +
                            container.getMappedPort(8080),
                    "storage.signingRegion=" + signingRegion,
                    "storage.accessKey=" + loadDefaultLibraryProperty(ACCESS_KEY),
                    "storage.secretKey=" + loadDefaultLibraryProperty(SECRET_KEY),
                    "storage.clientProtocol=" + clientProtocol,
                    "storage.clientMaxErrorRetry=" + clientMaxErrorRetry,
                    "storage.bucketName=" + bucketName)
                    .and(properties)
                    .applyTo(context);
        }
    }
}
