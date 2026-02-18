package dev.vality.testcontainers.annotations.util;

import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RandomBeans {

    public static <T> T random(Class<T> type, String... excludedFields) {
        var parameters = createParametersWithExcludedFields(excludedFields);
        var easyRandom = new EasyRandom(parameters);
        return easyRandom.nextObject(type);
    }

    public static <T> List<T> randomListOf(int amount, Class<T> type, String... excludedFields) {
        var parameters = createParametersWithExcludedFields(excludedFields);
        var easyRandom = new EasyRandom(parameters);
        return easyRandom.objects(type, amount).collect(Collectors.toList());
    }

    public static <T> Stream<T> randomStreamOf(int amount, Class<T> type, String... excludedFields) {
        var parameters = createParametersWithExcludedFields(excludedFields);
        var easyRandom = new EasyRandom(parameters);
        return easyRandom.objects(type, amount);
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

    private static EasyRandomParameters createParametersWithExcludedFields(String... excludedFields) {
        var parameters = new EasyRandomParameters();
        parameters.randomize(LocalDateTime.class, () -> {
            var dateTime = LocalDateTime.now();
            return dateTime.truncatedTo(ChronoUnit.MICROS);
        });
        parameters.randomize(Instant.class, () -> {
            var instant = Instant.now();
            return instant.truncatedTo(ChronoUnit.MICROS);
        });
        parameters.randomize(Date.class, () -> {
            var instant = Instant.now().truncatedTo(ChronoUnit.MICROS);
            return Date.from(instant);
        });
        parameters.randomize(Timestamp.class, () -> {
            var instant = Instant.now().truncatedTo(ChronoUnit.MICROS);
            return Timestamp.from(instant);
        });
        parameters.randomize(OffsetDateTime.class, () -> {
            var offsetDateTime = OffsetDateTime.now();
            return offsetDateTime.truncatedTo(ChronoUnit.MICROS);
        });
        parameters.randomize(ZonedDateTime.class, () -> {
            var zonedDateTime = ZonedDateTime.now();
            return zonedDateTime.truncatedTo(ChronoUnit.MICROS);
        });
        parameters.randomize(Calendar.class, () -> {
            var instant = Instant.now().truncatedTo(ChronoUnit.MICROS);
            var calendar = Calendar.getInstance();
            calendar.setTime(Date.from(instant));
            return calendar;
        });
        parameters.randomize(LocalDate.class, LocalDate::now);
        parameters.randomize(LocalTime.class, () -> {
            var time = LocalTime.now();
            return time.truncatedTo(ChronoUnit.MICROS);
        });
        if (excludedFields != null) {
            for (var excludedField : excludedFields) {
                parameters.excludeField(field -> field.getName().equals(excludedField));
            }
        }
        parameters.objectPoolSize(100)
                .randomizationDepth(3)
                .charset(StandardCharsets.UTF_8)
                .stringLengthRange(5, 50)
                .collectionSizeRange(1, 10);
        return parameters;
    }
}
