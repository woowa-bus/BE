package com.woowa.bus.search.application;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteException;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusStation;
import com.woowa.bus.message.BusMessageFormatter;
import java.util.Comparator;
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

            List<BusArrivalResult> arrivals = busArrivalClient.getArrivals(station.stationId()).stream()
                    .sorted(Comparator.comparing(BusArrivalResult::predictTime1, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(BusArrivalResult::busNumber))
                    .toList();
            log.debug("Arrival list loaded. stationName={}, stationId={}, arrivalCount={}",
                    station.name(), station.stationId(), arrivals.size());
            if (arrivals.isEmpty()) {
                log.warn("Station arrival list is empty. stationName={}, stationId={}", station.name(), station.stationId());
            }
            String message = BusMessageFormatter.stationArrival(station.name(), arrivals);
            log.info("Station arrival search completed. stationName={}, arrivalCount={}", station.name(), arrivals.size());
            return message;
        } catch (BusRouteException exception) {
            log.error("Station lookup failed. stationName={}", stationName, exception);
            return exception.getMessage();
        } catch (BusArrivalException exception) {
            log.warn("Station arrival search failed due to bus API error. stationName={}", stationName, exception);
            return BusMessageFormatter.busApiFailure();
        } catch (RuntimeException exception) {
            log.error("Station arrival search failed unexpectedly. stationName={}", stationName, exception);
            return BusMessageFormatter.busApiFailure();
        }
    }

    public String searchBus(String stationName, String busNumber) {
        try {
            log.info("Searching bus arrival. stationName={}, busNumber={}", stationName, busNumber);
            SupportedBusStation station = busRouteRegistry.station(stationName);
            log.debug("Station resolved for bus search. stationName={}, stationId={}", station.name(), station.stationId());

            BusArrivalResult arrival = findArrival(station.stationId(), busNumber);
            if (arrival == null) {
                log.info("Bus arrival search completed with no arrival info. stationName={}, busNumber={}", station.name(), busNumber);
                return BusMessageFormatter.noArrival(station.name(), busNumber);
            }

            log.info("Bus arrival search completed. stationName={}, busNumber={}, predictTime1={}, predictTime2={}",
                    station.name(), busNumber, arrival.predictTime1(), arrival.predictTime2());
            return BusMessageFormatter.busArrival(station.name(), busNumber, arrival);
        } catch (BusRouteException exception) {
            log.error("Bus search route lookup failed. stationName={}, busNumber={}", stationName, busNumber, exception);
            return exception.getMessage();
        } catch (BusArrivalException exception) {
            log.warn("Bus arrival search failed due to bus API error. stationName={}, busNumber={}", stationName, busNumber, exception);
            return BusMessageFormatter.busApiFailure();
        } catch (RuntimeException exception) {
            log.error("Bus arrival search failed unexpectedly. stationName={}, busNumber={}", stationName, busNumber, exception);
            return BusMessageFormatter.busApiFailure();
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
}
