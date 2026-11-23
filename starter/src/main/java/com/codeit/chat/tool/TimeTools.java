package com.codeit.chat.tool;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TimeTools {
    // TODO 2: 도구의 기능·사용 조건·범위와 각 인자의 의미·형식을 annotation으로 설명하세요.
    @Tool
    public TimeConversion convertTime(@ToolParam String dateTime, String sourceZone, String targetZone) {
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
