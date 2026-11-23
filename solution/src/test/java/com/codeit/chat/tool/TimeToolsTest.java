package com.codeit.chat.tool;

import java.time.DateTimeException;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeToolsTest {
    private final TimeTools tools = new TimeTools();

    @ParameterizedTest
    @CsvSource({
            "2026-07-15T15:00:00, Asia/Seoul, America/New_York, 2026-07-15T02:00-04:00[America/New_York]",
            "2026-01-15T15:00:00, Asia/Seoul, America/New_York, 2026-01-15T01:00-05:00[America/New_York]",
            "2026-07-15T00:30:00, Asia/Seoul, America/Los_Angeles, 2026-07-14T08:30-07:00[America/Los_Angeles]",
            "2026-07-15T15:00:00, Asia/Seoul, Asia/Seoul, 2026-07-15T15:00+09:00[Asia/Seoul]",
            "2026-07-15T20:00:00, UTC, Asia/Seoul, 2026-07-16T05:00+09:00[Asia/Seoul]"
    })
    void convertsTheSameInstantAcrossTimeZones(String dateTime, String source, String target, String expected) {
        TimeTools.TimeConversion result = tools.convertTime(dateTime, source, target);
        ZonedDateTime sourceTime = ZonedDateTime.parse(result.sourceTime());
        ZonedDateTime targetTime = ZonedDateTime.parse(result.targetTime());

        assertThat(targetTime).isEqualTo(ZonedDateTime.parse(expected));
        assertThat(sourceTime.toInstant()).isEqualTo(targetTime.toInstant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-03-08T02:30:00", "2026-11-01T01:30:00"})
    void rejectsNonexistentOrAmbiguousLocalTime(String dateTime) {
        assertThatThrownBy(() -> tools.convertTime(dateTime, "America/New_York", "Asia/Seoul"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DST");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "2026-07-15T15:00:00Z"})
    void rejectsInvalidLocalDateTime(String dateTime) {
        assertThatThrownBy(() -> tools.convertTime(dateTime, "Asia/Seoul", "UTC"))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    void rejectsUnknownTimeZone() {
        assertThatThrownBy(() -> tools.convertTime("2026-07-15T15:00:00", "Unknown/Zone", "UTC"))
                .isInstanceOf(DateTimeException.class);
    }
}
