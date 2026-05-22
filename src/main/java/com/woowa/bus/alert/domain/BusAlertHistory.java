package com.woowa.bus.alert.domain;

import java.time.LocalDateTime;

public record BusAlertHistory(
        Long id,
        String slackUserId,
        String stationName,
        String busNumber,
        Integer predictTime1,
        Integer predictTime2,
        LocalDateTime notifiedAt
) {

    public static BusAlertHistory record(
            String slackUserId,
            String stationName,
            String busNumber,
            Integer predictTime1,
            Integer predictTime2,
            LocalDateTime notifiedAt
    ) {
        return new BusAlertHistory(null, slackUserId, stationName, busNumber, predictTime1, predictTime2, notifiedAt);
    }
}
