package com.woowa.bus.alert.application.dto;

import com.woowa.bus.alert.domain.BusAlert;
import java.time.LocalTime;

public record BusAlertResponse(
        String stationName,
        String busNumber,
        int notifyBeforeMinutes,
        LocalTime startTime,
        LocalTime endTime
) {

    public static BusAlertResponse from(BusAlert alert) {
        return new BusAlertResponse(
                alert.stationName(),
                alert.busNumber(),
                alert.notifyBeforeMinutes(),
                alert.startTime(),
                alert.endTime()
        );
    }
}
