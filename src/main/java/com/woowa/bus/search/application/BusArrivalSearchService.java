package com.woowa.bus.search.application;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import org.springframework.stereotype.Service;

@Service
public class BusArrivalSearchService {

    private final BusRouteRegistry busRouteRegistry;
    private final BusArrivalClient busArrivalClient;

    public BusArrivalSearchService(BusRouteRegistry busRouteRegistry, BusArrivalClient busArrivalClient) {
        this.busRouteRegistry = busRouteRegistry;
        this.busArrivalClient = busArrivalClient;
    }

    public String searchStation(String stationName) {
        try {
            SupportedBusStation station = busRouteRegistry.station(stationName);
            String arrivals = station.routes()
                    .stream()
                    .map(route -> busArrivalLine(station, route))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("현재 도착 예정 정보가 없어요.");
            return """
                    🚌 %s 정류장 도착 정보

                    %s""".formatted(stationName, arrivals);
        } catch (BusArrivalException exception) {
            return busApiFailureMessage();
        }
    }

    public String searchBus(String stationName, String busNumber) {
        try {
            SupportedBusStation station = busRouteRegistry.station(stationName);
            SupportedBusRoute route = station.route(busNumber);
            BusArrivalResult arrival = busArrivalClient.getArrival(station.stationId(), route.routeId(), route.staOrder());
            if (!arrival.hasArrival()) {
                return noArrivalMessage(stationName, busNumber);
            }
            return """
                    🚌 %s번 버스 도착 정보

                    정류장: %s
                    첫 번째 버스: %s
                    두 번째 버스: %s""".formatted(
                    busNumber,
                    stationName,
                    arrivalText(arrival.predictTime1()),
                    arrivalText(arrival.predictTime2())
            );
        } catch (BusArrivalException exception) {
            return busApiFailureMessage();
        }
    }

    private String busArrivalLine(SupportedBusStation station, SupportedBusRoute route) {
        BusArrivalResult arrival = busArrivalClient.getArrival(station.stationId(), route.routeId(), route.staOrder());
        if (!arrival.hasArrival()) {
            return "%s번: 현재 도착 예정 정보가 없어요.".formatted(route.busNumber());
        }
        return "%s번: %s / 다음 %s".formatted(
                route.busNumber(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2())
        );
    }

    private String arrivalText(Integer predictTime) {
        if (predictTime == null) {
            return "정보 없음";
        }
        return "%d분 후".formatted(predictTime);
    }

    private String noArrivalMessage(String stationName, String busNumber) {
        return """
                현재 도착 예정 정보가 없어요.

                정류장: %s
                버스: %s번""".formatted(stationName, busNumber);
    }

    private String busApiFailureMessage() {
        return """
                버스 정보를 가져오지 못했어요.
                잠시 후 다시 시도해 주세요.""";
    }
}
