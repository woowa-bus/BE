package com.woowa.bus.search.application;

import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.util.List;

public record StationArrivalView(
        String stationName,
        List<BusArrivalResult> arrivals,
        String errorMessage
) {

    public static StationArrivalView success(String stationName, List<BusArrivalResult> arrivals) {
        return new StationArrivalView(stationName, List.copyOf(arrivals), null);
    }

    public static StationArrivalView error(String message) {
        return new StationArrivalView(null, List.of(), message);
    }

    public boolean isError() {
        return errorMessage != null;
    }
}
