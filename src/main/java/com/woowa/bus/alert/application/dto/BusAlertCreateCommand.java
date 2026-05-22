package com.woowa.bus.alert.application.dto;

import java.time.LocalTime;

public record BusAlertCreateCommand(
        String slackUserId,
        String stationName,
        String busNumber,
        int notifyBeforeMinutes,
        LocalTime startTime,
        LocalTime endTime
) {
}
