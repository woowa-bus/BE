package com.woowa.bus.arrival.domain;

import java.util.List;

public interface BusArrivalClient {

    default BusArrivalResult getArrival(String stationId, String routeId, String staOrder) {
        return getArrivals(stationId).stream()
                .findFirst()
                .orElse(new BusArrivalResult("", null, null));
    }

    List<BusArrivalResult> getArrivals(String stationId);
}
