package dev.vality.testcontainers.annotations.util;

import dev.vality.testcontainers.annotations.exception.IoException;
import dev.vality.testcontainers.annotations.exception.NoSuchFileException;
import lombok.Builder;
import lombok.Data;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringApplicationPropertiesLoader {

    public static String loadDefaultLibraryProperty(String key) {
        var tag = loadPropertiesByFile().get(key);
        if (tag == null) {
            tag = getSource(PropertiesFileParameters.builder()
                    .propertySourceLoader(YamlPropertySourceLoader::new)
                    .name("testcontainers-annotations.yml")
                    .build())
                    .get(key);
        }
        return String.valueOf(tag);
    }

    public static Properties loadFromSpringApplicationPropertiesFile(List<String> keys) {
        var fileProperties = loadPropertiesByFile();
        var filtered = fileProperties.entrySet().stream()
                .filter(entry -> keys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(filtered.keySet()).containsAll(keys);
        var properties = new Properties();
        properties.putAll(filtered);
        return properties;
    }

    private static Map<String, Object> loadPropertiesByFile() {
        var parameters = findPropertiesFileParameters();
        return getSource(parameters);
    }

    private static Map<String, Object> getSource(PropertiesFileParameters parameters) {
        var currentClass = SpringApplicationPropertiesLoader.class;
        var classPathResource = new ClassPathResource(parameters.getName(), currentClass.getClassLoader());
        try {
            //noinspection unchecked
            return ((Map<String, OriginTrackedValue>) parameters.getPropertySourceLoader().get()
                    .load(classPathResource.getFilename(), classPathResource)
                    .getFirst()
                    .getSource())
                    .entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException ex) {
            throw new IoException("Error when loading properties, ", ex);
        }
    }

    private static PropertiesFileParameters findPropertiesFileParameters() {
        var currentClass = SpringApplicationPropertiesLoader.class;
        return Stream.<Map.Entry<String, Supplier<PropertySourceLoader>>>of(
                        Map.entry("application.yml", YamlPropertySourceLoader::new),
                        Map.entry("application.yaml", YamlPropertySourceLoader::new),
                        Map.entry("application.properties", PropertiesPropertySourceLoader::new),
                        Map.entry("application.xml", PropertiesPropertySourceLoader::new)
                )
                .filter(entry -> currentClass.getResource("/" + entry.getKey()) != null)
                .findFirst()
                .map(entry -> PropertiesFileParameters.builder()
                        .propertySourceLoader(entry.getValue())
                        .name(entry.getKey())
                        .build())
                .orElseThrow(() -> new NoSuchFileException(
                        "Error loading configuration: " +
                                "src/main/resources/application.[yml|yaml|properties|xml] — " +
                                "file not found"
                ));
    }

    @Data
    @Builder
    private static class PropertiesFileParameters {

        private Supplier<PropertySourceLoader> propertySourceLoader;
        private String name;

    }
}
