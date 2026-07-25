package dev.vality.testcontainers.annotations.minio;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
import dev.vality.testcontainers.annotations.util.SharedTestResourceLock;
import dev.vality.testcontainers.annotations.util.TestExecutionLock;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

public class MinioTestcontainerExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final ConcurrentMap<Class<?>, ContainerReference> CONTAINERS = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        getOrStart(testClass, configuration(testClass));
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var configuration = configuration(testClass);
        if (configuration.cleanupBucket()) {
            TestExecutionLock.acquire(context);
            try {
                var reference = getOrStart(testClass, configuration);
                MinioBucketManager.cleanupBucket(
                        reference.container(),
                        configuration.bucketName(),
                        configuration.excludedPrefixes());
            } catch (RuntimeException ex) {
                TestExecutionLock.release(context);
                throw ex;
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var testClass = context.getRequiredTestClass();
        var reference = CONTAINERS.remove(testClass);
        try {
            if (reference != null && !reference.singleton()) {
                reference.container().stop();
            }
        } finally {
            if (reference != null && reference.singleton()) {
                SharedTestResourceLock.release(testClass);
            }
        }
    }

    private static ContainerReference getOrStart(Class<?> testClass, Configuration configuration) {
        return CONTAINERS.computeIfAbsent(testClass, ignored -> createAndStart(testClass, configuration));
    }

    private static ContainerReference createAndStart(Class<?> testClass, Configuration configuration) {
        if (configuration.singleton()) {
            SharedTestResourceLock.acquire(testClass);
        }
        try {
            var container = configuration.singleton()
                    ? MinioTestcontainerFactory.singletonContainer()
                    : MinioTestcontainerFactory.container();
            try {
                GenericContainerUtil.startContainer(container);
                MinioBucketManager.ensureBucket(container, configuration.bucketName());
                if (configuration.singleton() && configuration.cleanupBucket()) {
                    MinioBucketManager.cleanupBucket(
                            container,
                            configuration.bucketName(),
                            configuration.excludedPrefixes());
                }
                return new ContainerReference(container, configuration.singleton());
            } catch (RuntimeException ex) {
                try {
                    if (configuration.singleton()) {
                        MinioTestcontainerFactory.discardSingleton(container);
                    } else {
                        container.stop();
                    }
                } catch (RuntimeException stopException) {
                    ex.addSuppressed(stopException);
                }
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (configuration.singleton()) {
                SharedTestResourceLock.release(testClass);
            }
            throw ex;
        }
    }

    private static Configuration configuration(Class<?> testClass) {
        var prototype = findPrototypeAnnotation(testClass);
        if (prototype.isPresent()) {
            var annotation = prototype.get();
            return new Configuration(
                    false,
                    annotation.bucketName(),
                    annotation.cleanupBucket(),
                    List.copyOf(Arrays.asList(annotation.excludeCleanupPrefixes())),
                    List.copyOf(Arrays.asList(annotation.properties())));
        }
        var annotation = findSingletonAnnotation(testClass)
                .orElseThrow(() -> new IllegalStateException("MinIO test annotation not found"));
        return new Configuration(
                true,
                annotation.bucketName(),
                annotation.cleanupBucket(),
                List.copyOf(Arrays.asList(annotation.excludeCleanupPrefixes())),
                List.copyOf(Arrays.asList(annotation.properties())));
    }

    private static Optional<MinioTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainer.class);
    }

    private static Optional<MinioTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, MinioTestcontainerSingleton.class);
    }

    private static final class ContainerReference {

        private final GenericContainer<?> container;
        private final boolean singleton;

        private ContainerReference(GenericContainer<?> container, boolean singleton) {
            this.container = container;
            this.singleton = singleton;
        }

        private GenericContainer<?> container() {
            return container;
        }

        private boolean singleton() {
            return singleton;
        }

    }

    private record Configuration(
            boolean singleton,
            String bucketName,
            boolean cleanupBucket,
            List<String> excludedPrefixes,
            List<String> properties) {
    }

    public static class MinioTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            if (findPrototypeAnnotation(testClass).isEmpty() && findSingletonAnnotation(testClass).isEmpty()) {
                return null;
            }
            return new MinioContextCustomizer(testClass, configuration(testClass));
        }
    }

    private record MinioContextCustomizer(
            Class<?> testClass,
            Configuration configuration) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var container = getOrStart(testClass, configuration).container();
            var hostAndPort = container.getHost() + ":" + container.getMappedPort(9000);
            var endpoint = "http://" + hostAndPort + "/";
            var user = loadDefaultLibraryProperty(MinioTestcontainerFactory.MINIO_USER);
            var password = loadDefaultLibraryProperty(MinioTestcontainerFactory.MINIO_PASSWORD);
            TestPropertyValues.of(
                            "storage.endpoint=" + hostAndPort,
                            "storage.accessKey=" + user,
                            "storage.secretKey=" + password,
                            "storage.bucketName=" + configuration.bucketName(),
                            "s3.endpoint=" + hostAndPort,
                            "s3.bucket-name=" + configuration.bucketName(),
                            "s3.access-key=" + user,
                            "s3.secret-key=" + password,
                            "s3-sdk-v2.enabled=false",
                            "s3-sdk-v2.endpoint=" + endpoint,
                            "s3-sdk-v2.bucket-name=" + configuration.bucketName(),
                            "s3-sdk-v2.access-key=" + user,
                            "s3-sdk-v2.secret-key=" + password)
                    .and(configuration.properties().toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
