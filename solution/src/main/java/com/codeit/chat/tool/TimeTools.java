package com.codeit.chat.tool;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TimeTools {
    @Tool(description = """
            출발 지역의 날짜·시간을 도착 지역의 같은 순간으로 변환합니다.
            지역 간 시차를 계산할 때 사용합니다. 날짜와 출발·도착 지역이 필요합니다.
            일정 조회·예약·변경은 수행하지 않습니다.
            """)
    public TimeConversion convertTime(
            @ToolParam(description = "출발 지역의 날짜·시간. ISO-8601 형식. 예: 2026-07-15T15:00:00") String dateTime,
            @ToolParam(description = "출발 지역의 IANA time zone ID. 예: Asia/Seoul") String sourceZone,
            @ToolParam(description = "도착 지역의 IANA time zone ID. 예: America/New_York") String targetZone) {
        LocalDateTime localTime = LocalDateTime.parse(dateTime);
        ZoneId source = ZoneId.of(sourceZone);
        ZoneId target = ZoneId.of(targetZone);
        if (source.getRules().getValidOffsets(localTime).size() != 1) {
            throw new IllegalArgumentException("DST 전환으로 존재하지 않거나 중복되는 출발 시각입니다.");
        }
        ZonedDateTime sourceTime = localTime.atZone(source);
        ZonedDateTime targetTime = sourceTime.withZoneSameInstant(target);
        return new TimeConversion(sourceTime.toString(), targetTime.toString());
    }

    public record TimeConversion(String sourceTime, String targetTime) {
    }
}
