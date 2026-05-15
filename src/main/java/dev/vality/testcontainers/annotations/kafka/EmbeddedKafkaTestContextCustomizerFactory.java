package dev.vality.testcontainers.annotations.kafka;

import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;
import java.util.Optional;

public class EmbeddedKafkaTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) ->
                findAnnotation(testClass).ifPresent(annotation -> init(context, annotation));
    }

    private Optional<EmbeddedKafkaTest> findAnnotation(Class<?> testClass) {
        return AnnotationSupport.findAnnotation(testClass, EmbeddedKafkaTest.class);
    }

    private void init(ConfigurableApplicationContext context, EmbeddedKafkaTest annotation) {
        TestPropertyValues.of(
                        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                        "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                        "kafka.ssl.enabled=false")
                .and(annotation.properties())
                .applyTo(context);
    }
}
