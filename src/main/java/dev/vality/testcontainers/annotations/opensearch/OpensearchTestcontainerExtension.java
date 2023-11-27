package dev.vality.testcontainers.annotations.opensearch;

import dev.vality.testcontainers.annotations.util.GenericContainerUtil;
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

@Slf4j
public class OpensearchTestcontainerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ThreadLocal<GenericContainer<?>> THREAD_CONTAINER = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (findPrototypeAnnotation(context).isPresent()) {
            var container = OpensearchTestcontainerFactory.container();
            GenericContainerUtil.startContainer(container);
            THREAD_CONTAINER.set(container);
        } else if (findSingletonAnnotation(context).isPresent()) {
            var container = OpensearchTestcontainerFactory.singletonContainer();
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

    private static Optional<OpensearchTestcontainer> findPrototypeAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), OpensearchTestcontainer.class);
    }

    private static Optional<OpensearchTestcontainer> findPrototypeAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, OpensearchTestcontainer.class);
    }

    private static Optional<OpensearchTestcontainerSingleton> findSingletonAnnotation(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), OpensearchTestcontainerSingleton.class);
    }

    private static Optional<OpensearchTestcontainerSingleton> findSingletonAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, OpensearchTestcontainerSingleton.class);
    }

    public static class OpensearchTestcontainerContextCustomizerFactory implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(
                Class<?> testClass,
                List<ContextConfigurationAttributes> configAttributes) {
            return (context, mergedConfig) -> {
                if (findPrototypeAnnotation(testClass).isPresent()) {
                    init(context, findPrototypeAnnotation(testClass).get().properties()); //NOSONAR
                } else if (findSingletonAnnotation(testClass).isPresent()) {
                    init(context, findSingletonAnnotation(testClass).get().properties()); //NOSONAR
                }
            };
        }

        private void init(ConfigurableApplicationContext context, String[] properties) {
            var container = THREAD_CONTAINER.get();
            TestPropertyValues.of(
                            "opensearch.hostname=" + container.getHost(),
                            "opensearch.port=" + container.getFirstMappedPort())
                    .and(properties)
                    .applyTo(context);
        }
    }
}
