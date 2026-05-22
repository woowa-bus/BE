package com.woowa.bus.route.domain;

import java.util.List;

public record SupportedBusStation(
        String name,
        String stationId,
        List<SupportedBusRoute> routes
) {

    public static SupportedBusStation of(String name, String stationId, List<SupportedBusRoute> routes) {
        return new SupportedBusStation(name, stationId, List.copyOf(routes));
    }

    public List<String> busNumbers() {
        return routes.stream()
                .map(SupportedBusRoute::busNumber)
                .toList();
    }

    public SupportedBusRoute route(String busNumber) {
        return routes.stream()
                .filter(route -> route.busNumber().equals(busNumber))
                .findFirst()
                .orElseThrow(() -> new BusRouteException("""
                        %s 정류장에서 지원하지 않는 버스예요.

                        지원 가능한 버스:
                        %s""".formatted(name, String.join(", ", busNumbers()))));
    }
}
