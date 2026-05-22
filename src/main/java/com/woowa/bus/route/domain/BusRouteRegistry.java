package com.woowa.bus.route.domain;

import java.util.List;

public class BusRouteRegistry {

    private final List<SupportedBusStation> stations;

    private BusRouteRegistry(List<SupportedBusStation> stations) {
        this.stations = List.copyOf(stations);
    }

    public static BusRouteRegistry of(List<SupportedBusStation> stations) {
        return new BusRouteRegistry(stations);
    }

    public SupportedBusStation station(String stationName) {
        return stations.stream()
                .filter(station -> station.name().equals(stationName))
                .findFirst()
                .orElseThrow(() -> new BusRouteException("""
                        지원하지 않는 정류장이에요.

                        사용 가능한 정류장:
                        %s""".formatted(availableStationMessage())));
    }

    public SupportedBusRoute route(String stationName, String busNumber) {
        return station(stationName).route(busNumber);
    }

    public List<String> stationNames() {
        return stations.stream()
                .map(SupportedBusStation::name)
                .toList();
    }

    private String availableStationMessage() {
        return stations.stream()
                .map(station -> "- " + station.name())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
