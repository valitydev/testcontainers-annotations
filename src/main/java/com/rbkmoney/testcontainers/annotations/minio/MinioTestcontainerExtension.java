package com.rbkmoney.testcontainers.annotations.minio;

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

import static com.rbkmoney.testcontainers.annotations.minio.MinioTestcontainerFactory.MINIO_PASSWORD;
import static com.rbkmoney.testcontainers.annotations.minio.MinioTestcontainerFactory.MINIO_USER;
import static com.rbkmoney.testcontainers.annotations.util.GenericContainerUtil.startContainer;
import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class MinioTestcontainerExtension
        implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<GenericContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        var annotation = findMinioTestcontainerSingletonAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = MinioTestcontainerFactory.singletonContainer();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var annotation = findMinioTestcontainerAnnotation(context);
        if (!annotation.isPresent()) {
            return;
        }
        var container = MinioTestcontainerFactory.container();
        if (!container.isRunning()) {
            startContainer(container);
        }
        THREAD_CONTAINER.set(container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (findMinioTestcontainerAnnotation(context).isPresent()) {
            var container = THREAD_CONTAINER.get();
            if (container != null && container.isRunning()) {
                container.stop();
            }
            THREAD_CONTAINER.remove();
        } else if (findMinioTestcontainerSingletonAnnotation(context).isPresent()) {
            THREAD_CONTAINER.remove();
        }
    }

    private static Optional<MinioTestcontainer> findMinioTestcontainerAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), MinioTestcontainer.class);
    }

    private static Optional<MinioTestcontainer> findMinioTestcontainerAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainer.class);
    }

    private static Optional<MinioTestcontainerSingleton> findMinioTestcontainerSingletonAnnotation(
            ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), MinioTestcontainerSingleton.class);
    }

    private static Optional<MinioTestcontainerSingleton> findMinioTestcontainerSingletonAnnotation(
            Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainerSingleton.class);
    }

    public static class MinioTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                var minioTestcontainerAnnotation = findMinioTestcontainerAnnotation(testClass);
                if (minioTestcontainerAnnotation.isPresent()) {
                    var annotation = minioTestcontainerAnnotation.get();
                    init(
                            context,
                            annotation.signingRegion(),
                            annotation.clientProtocol(),
                            annotation.clientMaxErrorRetry(),
                            annotation.bucketName(),
                            annotation.properties());
                } else {
                    findMinioTestcontainerSingletonAnnotation(testClass).ifPresent(
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
                            container.getMappedPort(9000),
                    "storage.signingRegion=" + signingRegion,
                    "storage.accessKey=" + loadDefaultLibraryProperty(MINIO_USER),
                    "storage.secretKey=" + loadDefaultLibraryProperty(MINIO_PASSWORD),
                    "storage.clientProtocol=" + clientProtocol,
                    "storage.clientMaxErrorRetry=" + clientMaxErrorRetry,
                    "storage.bucketName=" + bucketName)
                    .and(properties)
                    .applyTo(context);
        }
    }
}
