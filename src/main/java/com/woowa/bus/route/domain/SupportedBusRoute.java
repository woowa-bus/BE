package com.woowa.bus.route.domain;

public record SupportedBusRoute(
        String busNumber,
        String routeId,
        String staOrder
) {

    public static SupportedBusRoute of(String busNumber, String routeId, String staOrder) {
        return new SupportedBusRoute(busNumber, routeId, staOrder);
    }
}
