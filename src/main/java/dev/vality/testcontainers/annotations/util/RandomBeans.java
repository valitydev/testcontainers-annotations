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
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RandomBeans {

    public static final long DEFAULT_SEED = 123L;
    private static final Instant BASE_INSTANT = Instant.parse("2020-01-01T00:00:00Z");


    public static <T> T random(Class<T> type, String... excludedFields) {
        var easyRandom = new EasyRandom(createParametersWithExcludedFields(
                randomSeed(), Clock.systemUTC(), excludedFields));
        return easyRandom.nextObject(type);
    }

    public static <T> T random(Long seed, Class<T> type, String... excludedFields) {
        var easyRandom = new EasyRandom(createDeterministicParameters(seed, excludedFields));
        return easyRandom.nextObject(type);
    }

    public static <T> List<T> randomListOf(int amount, Class<T> type, String... excludedFields) {
        var easyRandom = new EasyRandom(createParametersWithExcludedFields(
                randomSeed(), Clock.systemUTC(), excludedFields));
        return easyRandom.objects(type, amount).collect(Collectors.toList());
    }

    public static <T> List<T> randomListOf(Long seed, int amount, Class<T> type, String... excludedFields) {
        var easyRandom = new EasyRandom(createDeterministicParameters(seed, excludedFields));
        return easyRandom.objects(type, amount).collect(Collectors.toList());
    }

    public static <T> Stream<T> randomStreamOf(int amount, Class<T> type, String... excludedFields) {
        var easyRandom = new EasyRandom(createParametersWithExcludedFields(
                randomSeed(), Clock.systemUTC(), excludedFields));
        return easyRandom.objects(type, amount);
    }

    @SneakyThrows
    public static <T extends TBase<?, ?>> T randomThrift(Class<T> type) {
        return randomThrift(type, Clock.systemUTC(), MockMode.ALL);
    }

    @SneakyThrows
    public static <T extends TBase<?, ?>> T randomThrift(Class<T> type, Clock clock) {
        return randomThrift(type, clock, MockMode.ALL);
    }

    @SneakyThrows
    private static <T extends TBase<?, ?>> T randomThrift(
            Class<T> type,
            Clock clock,
            MockMode mode) {
        var mockTBaseProcessor = new MockTBaseProcessor(mode, 25, 1);
        mockTBaseProcessor.addFieldHandler(
                structHandler -> structHandler.value(Instant.now(clock).toString()),
                "created_at", "at", "due");
        return mockTBaseProcessor.process(type.getConstructor().newInstance(), new TBaseHandler<>(type));
    }

    @SneakyThrows
    public static <T extends TBase<?, ?>> T randomThriftOnlyRequiredFields(Class<T> type) {
        return randomThrift(type, Clock.systemUTC(), MockMode.REQUIRED_ONLY);
    }

    private static EasyRandomParameters createDeterministicParameters(Long seed, String... excludedFields) {
        if (seed == null) {
            throw new IllegalArgumentException("Seed must not be null");
        }
        return createParametersWithExcludedFields(seed, deterministicClock(seed), excludedFields);
    }

    private static EasyRandomParameters createParametersWithExcludedFields(
            Long seed,
            Clock clock,
            String... excludedFields) {
        var instant = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        var localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        var parameters = new EasyRandomParameters();
        parameters.randomize(LocalDateTime.class, () -> localDateTime);
        parameters.randomize(Instant.class, () -> instant);
        parameters.randomize(Date.class, () -> Date.from(instant));
        parameters.randomize(Timestamp.class, () -> Timestamp.from(instant));
        parameters.randomize(OffsetDateTime.class, () -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
        parameters.randomize(ZonedDateTime.class, () -> ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
        parameters.randomize(Calendar.class, () -> {
            var calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTime(Date.from(instant));
            return calendar;
        });
        parameters.randomize(LocalDate.class, localDateTime::toLocalDate);
        parameters.randomize(LocalTime.class, localDateTime::toLocalTime);
        if (excludedFields != null) {
            for (var excludedField : excludedFields) {
                parameters.excludeField(field -> field.getName().equals(excludedField));
            }
        }
        parameters.seed(seed)
                .objectPoolSize(100)
                .randomizationDepth(3)
                .charset(StandardCharsets.UTF_8)
                .stringLengthRange(5, 50)
                .collectionSizeRange(1, 10);
        return parameters;
    }

    private static Clock deterministicClock(long seed) {
        var secondsInTenYears = 3650L * 24 * 60 * 60;
        var offsetSeconds = Math.floorMod(seed, secondsInTenYears);
        return Clock.fixed(BASE_INSTANT.plusSeconds(offsetSeconds), ZoneOffset.UTC);
    }

    private static long randomSeed() {
        return ThreadLocalRandom.current().nextLong();
    }
}
