package dev.vality.testcontainers.annotations.util;

import dev.vality.geck.common.util.TypeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValuesGenerator {

    private static final LocalDateTime fromTime = LocalDateTime.now().minusHours(3);
    private static final LocalDateTime toTime = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime inFromToPeriodTime = LocalDateTime.now().minusHours(2);

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String generateDate() {
        return TypeUtil.temporalToString(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
    }

    public static Long generateLong() {
        return RandomBeans.random(Long.class);
    }

    public static Integer generateInt() {
        return RandomBeans.random(Integer.class);
    }

    public static String generateString() {
        return RandomBeans.random(String.class);
    }

    public static LocalDateTime generateLocalDateTime() {
        return RandomBeans.random(LocalDateTime.class).truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateInstant() {
        return RandomBeans.random(Instant.class).truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateCurrentTimePlusDay() {
        return LocalDateTime.now().plusDays(1).toInstant(getZoneOffset()).truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant generateCurrentTimePlusSecond() {
        return LocalDateTime.now().plusSeconds(1).toInstant(getZoneOffset()).truncatedTo(ChronoUnit.MICROS);
    }

    public static ZoneOffset getZoneOffset() {
        return ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now());
    }

    public static String getContent(InputStream content) throws IOException {
        return IOUtils.toString(content, StandardCharsets.UTF_8);
    }

    public static LocalDateTime getFromTime() {
        return fromTime.truncatedTo(ChronoUnit.MICROS);
    }

    public static LocalDateTime getToTime() {
        return toTime.truncatedTo(ChronoUnit.MICROS);
    }

    public static LocalDateTime getInFromToPeriodTime() {
        return inFromToPeriodTime.truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant getCurrentInstant() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
