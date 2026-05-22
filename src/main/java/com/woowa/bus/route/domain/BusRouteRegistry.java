package com.woowa.bus.route.domain;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusRouteRegistry {

    private final SupportedBusRouteRepository repository;

    public BusRouteRegistry(SupportedBusRouteRepository repository) {
        this.repository = repository;
    }

    public static BusRouteRegistry of(List<SupportedBusStation> stations) {
        return new BusRouteRegistry(() -> List.copyOf(stations));
    }

    public SupportedBusStation station(String stationName) {
        log.debug("Looking up supported station. stationName={}", stationName);
        List<SupportedBusStation> stations = stations();
        log.debug("Supported stations loaded. count={}, stationNames={}", stations.size(), stations.stream().map(SupportedBusStation::name).toList());
        return stations.stream()
                .filter(station -> station.name().equals(stationName))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Supported station not found. stationName={}, availableStations={}", stationName, stations.stream().map(SupportedBusStation::name).toList());
                    return new BusRouteException("""
                        지원하지 않는 정류장이에요.

                        사용 가능한 정류장:
                        %s""".formatted(availableStationMessage()));
                });
    }

    public SupportedBusRoute route(String stationName, String busNumber) {
        log.debug("Looking up supported route. stationName={}, busNumber={}", stationName, busNumber);
        SupportedBusRoute route = station(stationName).route(busNumber);
        log.debug("Supported route found. stationName={}, busNumber={}, routeId={}, staOrder={}",
                stationName, busNumber, route.routeId(), route.staOrder());
        return route;
    }

    public List<String> stationNames() {
        log.debug("Loading supported station names.");
        return stations().stream()
                .map(SupportedBusStation::name)
                .toList();
    }

    private String availableStationMessage() {
        return stations().stream()
                .map(station -> "- " + station.name())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<SupportedBusStation> stations() {
        return repository.findAll();
    }
}
