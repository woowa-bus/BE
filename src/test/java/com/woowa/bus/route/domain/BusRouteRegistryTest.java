package com.woowa.bus.route.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BusRouteRegistryTest {

    @Test
    void station_success() {
        BusRouteRegistry registry = BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12"),
                        SupportedBusRoute.of("55", "234000002", "13")
                )),
                SupportedBusStation.of("벤처타운(북문)", "200000002", List.of(
                        SupportedBusRoute.of("310", "234000003", "14")
                ))
        ));

        SupportedBusStation station = registry.station("텔레칩스");

        assertEquals("200000001", station.stationId());
        assertEquals(List.of("310", "55"), station.busNumbers());
    }

    @Test
    void station_success_with_alias_name() {
        BusRouteRegistry registry = BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                )),
                SupportedBusStation.of("벤처타운(북문)", "200000002", List.of(
                        SupportedBusRoute.of("310", "234000003", "14")
                ))
        ));

        SupportedBusStation station = registry.station("벤처타워 북문");

        assertEquals("200000002", station.stationId());
        assertEquals("벤처타운(북문)", station.name());
    }

    @Test
    void station_fail_with_unsupported_station() {
        BusRouteRegistry registry = BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                ))
        ));

        BusRouteException exception = assertThrows(BusRouteException.class, () -> registry.station("강남역"));

        assertEquals("""
                ⚠️ *지원하지 않는 정류장이에요*

                사용 가능한 정류장:
                • 텔레칩스""", exception.getMessage());
    }

    @Test
    void route_fail_with_unsupported_bus_number() {
        BusRouteRegistry registry = BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12"),
                        SupportedBusRoute.of("55", "234000002", "13")
                ))
        ));

        BusRouteException exception = assertThrows(BusRouteException.class, () -> registry.route("텔레칩스", "9999"));

        assertEquals("""
                ⚠️ *텔레칩스 정류장에서 지원하지 않는 버스예요*

                지원 가능한 버스:
                • 310
                • 55""", exception.getMessage());
    }
}
