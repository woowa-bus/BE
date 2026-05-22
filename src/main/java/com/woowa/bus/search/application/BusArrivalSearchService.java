package com.woowa.bus.search.application;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteException;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BusArrivalSearchService {

    private final BusRouteRegistry busRouteRegistry;
    private final BusArrivalClient busArrivalClient;

    public BusArrivalSearchService(BusRouteRegistry busRouteRegistry, BusArrivalClient busArrivalClient) {
        this.busRouteRegistry = busRouteRegistry;
        this.busArrivalClient = busArrivalClient;
    }

    public String searchStation(String stationName) {
        try {
            log.info("Searching station arrival. stationName={}", stationName);
            SupportedBusStation station = busRouteRegistry.station(stationName);
            log.debug("Station resolved. stationName={}, stationId={}", station.name(), station.stationId());

            List<BusArrivalResult> arrivals = busArrivalClient.getArrivals(station.stationId());
            log.debug("Arrival list loaded. stationName={}, stationId={}, arrivalCount={}",
                    stationName, station.stationId(), arrivals.size());

            if (arrivals.isEmpty()) {
                log.warn("Station arrival list is empty. stationName={}, stationId={}", stationName, station.stationId());
                return """
                        등록된 버스 정보가 없어요.

                        정류장: %s""".formatted(stationName);
            }

            String message = arrivals.stream()
                    .map(this::arrivalLine)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("현재 도착 예정 정보가 없어요.");
            log.info("Station arrival search completed. stationName={}, arrivalCount={}", stationName, arrivals.size());
            return """
                    🚌 %s 정류장 도착 정보

                    %s""".formatted(stationName, message);
        } catch (BusRouteException exception) {
            log.error("Station lookup failed. stationName={}", stationName, exception);
            return exception.getMessage();
        } catch (BusArrivalException exception) {
            log.warn("Station arrival search failed due to bus API error. stationName={}", stationName, exception);
            return busApiFailureMessage();
        } catch (RuntimeException exception) {
            log.error("Station arrival search failed unexpectedly. stationName={}", stationName, exception);
            return busApiFailureMessage();
        }
    }

    public String searchBus(String stationName, String busNumber) {
        try {
            log.info("Searching bus arrival. stationName={}, busNumber={}", stationName, busNumber);
            SupportedBusStation station = busRouteRegistry.station(stationName);
            log.debug("Station resolved for bus search. stationName={}, stationId={}", station.name(), station.stationId());

            BusArrivalResult arrival = findArrival(station.stationId(), busNumber);
            if (arrival == null) {
                log.info("Bus arrival search completed with no arrival info. stationName={}, busNumber={}", stationName, busNumber);
                return noArrivalMessage(stationName, busNumber);
            }

            log.info("Bus arrival search completed. stationName={}, busNumber={}, predictTime1={}, predictTime2={}",
                    stationName, busNumber, arrival.predictTime1(), arrival.predictTime2());
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
        } catch (BusRouteException exception) {
            log.error("Bus search route lookup failed. stationName={}, busNumber={}", stationName, busNumber, exception);
            return exception.getMessage();
        } catch (BusArrivalException exception) {
            log.warn("Bus arrival search failed due to bus API error. stationName={}, busNumber={}", stationName, busNumber, exception);
            return busApiFailureMessage();
        } catch (RuntimeException exception) {
            log.error("Bus arrival search failed unexpectedly. stationName={}, busNumber={}", stationName, busNumber, exception);
            return busApiFailureMessage();
        }
    }

    private BusArrivalResult findArrival(String stationId, String busNumber) {
        log.debug("Finding arrival by bus number. stationId={}, busNumber={}", stationId, busNumber);
        return busArrivalClient.getArrivals(stationId)
                .stream()
                .peek(arrival -> log.debug("Arrival candidate. stationId={}, busNumber={}, predictTime1={}, predictTime2={}",
                        stationId, arrival.busNumber(), arrival.predictTime1(), arrival.predictTime2()))
                .filter(arrival -> busNumber.equals(arrival.busNumber()))
                .findFirst()
                .orElse(null);
    }

    private String arrivalLine(BusArrivalResult arrival) {
        log.debug("Arrival line fetched. busNumber={}, predictTime1={}, predictTime2={}",
                arrival.busNumber(), arrival.predictTime1(), arrival.predictTime2());
        if (!arrival.hasArrival()) {
            return "%s번: 현재 도착 예정 정보가 없어요.".formatted(arrival.busNumber());
        }
        return "%s번: %s / 다음 %s".formatted(
                arrival.busNumber(),
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
