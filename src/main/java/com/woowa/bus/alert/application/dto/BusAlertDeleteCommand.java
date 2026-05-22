package com.woowa.bus.alert.application.dto;

public record BusAlertDeleteCommand(
        String slackUserId,
        String stationName,
        String busNumber
) {
}
