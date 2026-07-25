package dev.vality.testcontainers.annotations.util;

import dev.vality.testcontainers.annotations.exception.IoException;
import dev.vality.testcontainers.annotations.exception.NoSuchFileException;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class SpringApplicationPropertiesLoader {

    private static final List<FileType> TYPES = List.of(
            new FileType("yml", YamlPropertySourceLoader::new),
            new FileType("yaml", YamlPropertySourceLoader::new),
            new FileType("properties", PropertiesPropertySourceLoader::new),
            new FileType("xml", PropertiesPropertySourceLoader::new));

    public static String loadDefaultLibraryProperty(String key) {
        return findExternalValue(key)
                .or(() -> findValue(loadApplicationProperties(false), key))
                .or(() -> findValue(loadNamedProperties("testcontainers-annotations", true), key))
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("Required property is not configured: " + key));
    }

    public static Properties loadFromSpringApplicationPropertiesFile(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new Properties();
        }
        var applicationProperties = loadApplicationProperties(true);
        var result = new Properties();
        var missing = new ArrayList<String>();
        for (var key : keys) {
            var value = findExternalValue(key)
                    .or(() -> findValue(applicationProperties, key));
            if (value.isPresent()) {
                result.setProperty(key, String.valueOf(value.get()));
            } else {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Required application properties are missing: " + missing);
        }
        return result;
    }

    private static Map<String, Object> loadApplicationProperties(boolean required) {
        var baseDocuments = loadNamedPropertyDocuments("application", required);
        var unconditionalProperties = mergeDocuments(baseDocuments, List.of(), false);
        var profiles = activeProfiles(unconditionalProperties);

        var properties = new LinkedHashMap<>(mergeDocuments(baseDocuments, profiles, true));
        profiles.forEach(profile -> properties.putAll(mergeDocuments(
                loadNamedPropertyDocuments("application-" + profile, false),
                profiles,
                true)));
        return properties;
    }

    private static Map<String, Object> loadNamedProperties(String baseName, boolean required) {
        var result = new LinkedHashMap<String, Object>();
        loadNamedPropertyDocuments(baseName, required).forEach(result::putAll);
        return result;
    }

    private static List<Map<String, Object>> loadNamedPropertyDocuments(String baseName, boolean required) {
        var documents = new ArrayList<Map<String, Object>>();
        var found = false;
        for (var type : TYPES) {
            var resource = new ClassPathResource(baseName + "." + type.extension());
            if (!resource.exists()) {
                continue;
            }
            found = true;
            try {
                var loader = type.loader().get();
                var propertySources = loader.load(resource.getFilename(), resource);
                for (var propertySource : propertySources) {
                    var document = new LinkedHashMap<String, Object>();
                    mergePropertySource(document, propertySource);
                    documents.add(document);
                }
            } catch (IOException ex) {
                throw new IoException("Error while loading " + resource.getPath(), ex);
            }
        }
        if (required && !found) {
            throw new NoSuchFileException(
                    "Configuration file not found: " + baseName + ".[yml|yaml|properties|xml]");
        }
        return documents;
    }

    private static Map<String, Object> mergeDocuments(
            List<Map<String, Object>> documents,
            List<String> activeProfiles,
            boolean includeActiveDocuments) {
        var result = new LinkedHashMap<String, Object>();
        for (var document : documents) {
            var activation = profileActivation(document);
            if (activation.isEmpty() || includeActiveDocuments && matchesProfiles(activation.get(), activeProfiles)) {
                result.putAll(document);
            }
        }
        return result;
    }

    private static Optional<String> profileActivation(Map<String, Object> document) {
        return findValue(document, "spring.config.activate.on-profile")
                .or(() -> findValue(document, "spring.profiles"))
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }

    private static boolean matchesProfiles(String expression, List<String> activeProfiles) {
        var expressions = Arrays.stream(expression.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
        return expressions.length > 0 && Profiles.of(expressions).matches(activeProfiles::contains);
    }

    private static void mergePropertySource(Map<String, Object> target, PropertySource<?> propertySource) {
        if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
            for (var propertyName : enumerable.getPropertyNames()) {
                var value = enumerable.getProperty(propertyName);
                if (value != null) {
                    target.put(propertyName, unwrap(value));
                }
            }
            return;
        }
        if (propertySource.getSource() instanceof Map<?, ?> source) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    target.put(String.valueOf(key), unwrap(value));
                }
            });
        }
    }

    private static Object unwrap(Object value) {
        return value instanceof OriginTrackedValue originTrackedValue
                ? originTrackedValue.getValue()
                : value;
    }

    private static List<String> activeProfiles(Map<String, Object> applicationProperties) {
        var profiles = new LinkedHashSet<String>();
        addProfiles(
                profiles,
                findExternalValue("spring.profiles.active")
                        .or(() -> findValue(applicationProperties, "spring.profiles.active")));
        addProfiles(
                profiles,
                findExternalValue("spring.profiles.include")
                        .or(() -> findValue(applicationProperties, "spring.profiles.include")));
        if (profiles.isEmpty()) {
            profiles.add("default");
        }
        return List.copyOf(profiles);
    }

    private static void addProfiles(Set<String> target, Optional<Object> value) {
        value.map(String::valueOf)
                .stream()
                .flatMap(profiles -> Arrays.stream(profiles.split(",")))
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .forEach(target::add);
    }

    private static Optional<Object> findValue(Map<String, Object> source, String key) {
        return Optional.ofNullable(source.get(key));
    }

    private static Optional<Object> findExternalValue(String key) {
        var systemProperty = System.getProperty(key);
        if (systemProperty != null) {
            return Optional.of(systemProperty);
        }
        var environmentKey = key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        return Optional.ofNullable(System.getenv(environmentKey));
    }

    private record FileType(String extension, Supplier<PropertySourceLoader> loader) {
    }
}
