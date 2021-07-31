package com.rbkmoney.testcontainers.annotations.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.benas.randombeans.EnhancedRandomBuilder.aNewEnhancedRandom;

public class RandomBeans {

    public static <T> T random(Class<T> type, String... excludedFields) {
        return aNewEnhancedRandom().nextObject(type, excludedFields);
    }

    public static <T> List<T> randomListOf(int amount, Class<T> type, String... excludedFields) {
        return aNewEnhancedRandom().objects(type, amount, excludedFields).collect(Collectors.toList());
    }

    public static <T> Stream<T> randomStreamOf(int amount, Class<T> type, String... excludedFields) {
        return aNewEnhancedRandom().objects(type, amount, excludedFields);
    }
}
