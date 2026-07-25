package dev.vality.testcontainers.annotations.kafka;

import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.List;

public class EmbeddedKafkaTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return AnnotationSupport.findAnnotation(testClass, EmbeddedKafkaTest.class)
                .<ContextCustomizer>map(annotation -> new EmbeddedKafkaContextCustomizer(
                        List.copyOf(Arrays.asList(annotation.properties()))))
                .orElse(null);
    }

    private record EmbeddedKafkaContextCustomizer(List<String> properties) implements ContextCustomizer {

        @Override
        public void customizeContext(
                ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            TestPropertyValues.of(
                            "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                            "kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                            "kafka.ssl.enabled=false")
                    .and(properties.toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
