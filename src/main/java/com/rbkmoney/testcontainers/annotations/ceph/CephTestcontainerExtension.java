package com.rbkmoney.testcontainers.annotations.ceph;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
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
import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.startContainer;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class CephTestcontainerExtension
        implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<GenericContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        var annotation = findCephTestcontainerSingletonAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = CephTestcontainerFactory.singletonContainer();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var annotation = findCephTestcontainerAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = CephTestcontainerFactory.container();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findCephTestcontainerAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findCephTestcontainerSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<CephTestcontainer> findCephTestcontainerAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), CephTestcontainer.class);
    }

    private static Optional<CephTestcontainer> findCephTestcontainerAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, CephTestcontainer.class);
    }

    private static Optional<CephTestcontainerSingleton> findCephTestcontainerSingletonAnnotation(
            ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), CephTestcontainerSingleton.class);
    }

    private static Optional<CephTestcontainerSingleton> findCephTestcontainerSingletonAnnotation(
            Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, CephTestcontainerSingleton.class);
    }

    public static class CephTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                var cephTestcontainerAnnotation = findCephTestcontainerAnnotation(testClass);
                if (cephTestcontainerAnnotation.isPresent()) {
                    var annotation = cephTestcontainerAnnotation.get();
                    init(
                            context,
                            annotation.signingRegion(),
                            annotation.clientProtocol(),
                            annotation.clientMaxErrorRetry(),
                            annotation.bucketName(),
                            annotation.properties());
                } else {
                    findCephTestcontainerSingletonAnnotation(testClass).ifPresent(
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
                    "storage.endpoint=" + container.getContainerIpAddress() + ":"
                            + container.getMappedPort(8080),
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
