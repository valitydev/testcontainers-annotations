package dev.vality.testcontainers.annotations.util;

import dev.vality.geck.common.util.TypeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValuesGenerator {

    private static final AtomicLong SEED_SEQUENCE = new AtomicLong(RandomBeans.DEFAULT_SEED);
    private static volatile Clock clock = Clock.systemDefaultZone();

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String generateDate() {
        return TypeUtil.temporalToString(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS));
    }

    public static Long generateLong() {
        return RandomBeans.random(SEED_SEQUENCE.getAndIncrement(), Long.class);
    }

    public static Integer generateInt() {
        return RandomBeans.random(SEED_SEQUENCE.getAndIncrement(), Integer.class);
    }

    public static String generateString() {
        return RandomBeans.random(SEED_SEQUENCE.getAndIncrement(), String.class);
    }

    public static LocalDateTime generateLocalDateTime() {
        return RandomBeans.random(SEED_SEQUENCE.getAndIncrement(), LocalDateTime.class)
                .truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateInstant() {
        return RandomBeans.random(SEED_SEQUENCE.getAndIncrement(), Instant.class)
                .truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateCurrentTimePlusDay() {
        return ZonedDateTime.now(clock).plusDays(1).toInstant().truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateCurrentTimePlusSecond() {
        return ZonedDateTime.now(clock).plusSeconds(1).toInstant().truncatedTo(ChronoUnit.MICROS);
    }

    public static ZoneOffset getZoneOffset() {
        return ZonedDateTime.now(clock).getOffset();
    }

    public static String getContent(InputStream content) throws IOException {
        try (content) {
            return IOUtils.toString(content, StandardCharsets.UTF_8);
        }
    }

    public static LocalDateTime getFromTime() {
        return LocalDateTime.now(clock).minusHours(3).truncatedTo(ChronoUnit.MICROS);
    }

    public static LocalDateTime getToTime() {
        return LocalDateTime.now(clock).minusHours(1).truncatedTo(ChronoUnit.MICROS);
    }

    public static LocalDateTime getInFromToPeriodTime() {
        return LocalDateTime.now(clock).minusHours(2).truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant getCurrentInstant() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    public static void useClock(Clock customClock) {
        if (customClock == null) {
            throw new IllegalArgumentException("Clock must not be null");
        }
        clock = customClock;
    }

    public static void resetClock() {
        clock = Clock.systemDefaultZone();
    }
}
