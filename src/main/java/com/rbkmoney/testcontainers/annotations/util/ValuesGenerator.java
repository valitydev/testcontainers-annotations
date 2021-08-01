package com.rbkmoney.testcontainers.annotations.util;

import com.rbkmoney.geck.common.util.TypeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.rbkmoney.testcontainers.annotations.util.RandomBeans.random;
import static java.time.LocalDateTime.now;
import static java.time.ZoneId.systemDefault;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValuesGenerator {

    private static final LocalDateTime fromTime = LocalDateTime.now().minusHours(3);
    private static final LocalDateTime toTime = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime inFromToPeriodTime = LocalDateTime.now().minusHours(2);

    public static String generateDate() {
        return TypeUtil.temporalToString(LocalDateTime.now());
    }

    public static Long generateLong() {
        return random(Long.class);
    }

    public static Integer generateInt() {
        return random(Integer.class);
    }

    public static String generateString() {
        return random(String.class);
    }

    public static LocalDateTime generateLocalDateTime() {
        return random(LocalDateTime.class);
    }

    public static Instant generateCurrentTimePlusDay() {
        return now().plusDays(1).toInstant(getZoneOffset());
    }

    public static ZoneOffset getZoneOffset() {
        return systemDefault().getRules().getOffset(now());
    }

    public static String getContent(InputStream content) throws IOException {
        return IOUtils.toString(content, StandardCharsets.UTF_8);
    }

    public static LocalDateTime getFromTime() {
        return fromTime;
    }

    public static LocalDateTime getToTime() {
        return toTime;
    }

    public static LocalDateTime getInFromToPeriodTime() {
        return inFromToPeriodTime;
    }

    public static Instant generateCurrentTimePlusSecond() {
        return LocalDateTime.now().plusSeconds(1).toInstant(getZoneOffset());
    }
}
