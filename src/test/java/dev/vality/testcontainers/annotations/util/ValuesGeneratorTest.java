package dev.vality.testcontainers.annotations.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesGeneratorTest {

    @AfterEach
    void resetClock() {
        ValuesGenerator.resetClock();
    }

    @Test
    void shouldAddCalendarDayAcrossDstTransition() {
        var clock = Clock.fixed(
                Instant.parse("2026-03-28T12:00:00Z"),
                ZoneId.of("Europe/Amsterdam"));
        ValuesGenerator.useClock(clock);

        assertThat(ValuesGenerator.generateCurrentTimePlusDay())
                .isEqualTo(Instant.parse("2026-03-29T11:00:00Z"));
    }

    @Test
    void shouldCalculateTimeWindowAtCallTime() {
        ValuesGenerator.useClock(Clock.fixed(Instant.parse("2026-07-25T10:00:00Z"), ZoneId.of("UTC")));

        assertThat(ValuesGenerator.getFromTime().toString()).isEqualTo("2026-07-25T07:00");
        assertThat(ValuesGenerator.getInFromToPeriodTime().toString()).isEqualTo("2026-07-25T08:00");
        assertThat(ValuesGenerator.getToTime().toString()).isEqualTo("2026-07-25T09:00");
    }
}
