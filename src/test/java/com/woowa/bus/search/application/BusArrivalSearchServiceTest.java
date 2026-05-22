package com.woowa.bus.search.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusArrivalSearchServiceTest {

    @Test
    void searchStation_success() {
        BusArrivalSearchService service = new BusArrivalSearchService(registry(), new FakeBusArrivalClient());

        String message = service.searchStation("텔레칩스");

        assertEquals("""
                🚌 텔레칩스 정류장 도착 정보

                • 310번: 4분 후 / 다음 13분 후
                • 55번: 7분 후 / 다음 18분 후""", message);
    }

    @Test
    void searchStation_fail_when_station_has_no_routes() {
        BusArrivalSearchService service = new BusArrivalSearchService(
                BusRouteRegistry.of(List.of(
                        SupportedBusStation.of("텔레칩스", "200000001", List.of())
                )),
                new EmptyBusArrivalClient()
        );

        String message = service.searchStation("텔레칩스");

        assertEquals("""
                등록된 버스 정보가 없어요.

                정류장: 텔레칩스""", message);
    }

    @Test
    void searchBus_success() {
        BusArrivalSearchService service = new BusArrivalSearchService(registry(), new FakeBusArrivalClient());

        String message = service.searchBus("텔레칩스", "310");

        assertEquals("""
                🚌 310번 버스 도착 정보

                정류장: 텔레칩스
                첫 번째 버스: 4분 후
                두 번째 버스: 13분 후""", message);
    }

    @Test
    void searchBus_fail_when_bus_api_fails() {
        BusArrivalSearchService service = new BusArrivalSearchService(registry(), new FailingBusArrivalClient());

        String message = service.searchBus("텔레칩스", "310");

        assertEquals("""
                버스 정보를 가져오지 못했어요.
                잠시 후 다시 시도해 주세요.""", message);
    }

    private BusRouteRegistry registry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12"),
                        SupportedBusRoute.of("55", "234000002", "13")
                ))
        ));
    }

    private static class FakeBusArrivalClient implements BusArrivalClient {

        @Override
        public List<BusArrivalResult> getArrivals(String stationId) {
            return List.of(
                    new BusArrivalResult("310", 4, 13),
                    new BusArrivalResult("55", 7, 18)
            );
        }
    }

    private static class FailingBusArrivalClient implements BusArrivalClient {

        @Override
        public List<BusArrivalResult> getArrivals(String stationId) {
            throw new BusArrivalException("버스 정보를 가져오지 못했어요.", new RuntimeException());
        }
    }

    private static class EmptyBusArrivalClient implements BusArrivalClient {

        @Override
        public List<BusArrivalResult> getArrivals(String stationId) {
            return List.of();
        }
    }
}
