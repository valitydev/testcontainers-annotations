package com.rbkmoney.testcontainers.annotations.util;

import com.rbkmoney.geck.serializer.kit.mock.MockMode;
import com.rbkmoney.geck.serializer.kit.mock.MockTBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import lombok.SneakyThrows;
import lombok.var;
import org.apache.thrift.TBase;

import java.time.Instant;
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

    @SneakyThrows
    public static <T extends TBase<?, ?>> T randomThrift(Class<T> type) {
        var mockTBaseProcessor = new MockTBaseProcessor(MockMode.ALL, 25, 1);
        mockTBaseProcessor.addFieldHandler(
                structHandler -> structHandler.value(Instant.now().toString()),
                "created_at", "at", "due");
        return mockTBaseProcessor.process(type.getConstructor().newInstance(), new TBaseHandler<>(type));
    }

    @SneakyThrows
    public static <T extends TBase<?, ?>> T randomThriftOnlyRequiredFields(Class<T> type) {
        var mockTBaseProcessor = new MockTBaseProcessor(MockMode.REQUIRED_ONLY, 25, 1);
        mockTBaseProcessor.addFieldHandler(
                structHandler -> structHandler.value(Instant.now().toString()),
                "created_at", "at", "due");
        return mockTBaseProcessor.process(type.getConstructor().newInstance(), new TBaseHandler<>(type));
    }
}
