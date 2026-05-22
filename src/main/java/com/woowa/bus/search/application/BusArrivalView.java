package com.woowa.bus.search.application;

import com.woowa.bus.arrival.domain.BusArrivalResult;

public record BusArrivalView(
        String stationName,
        String busNumber,
        BusArrivalResult arrival,
        String errorMessage
) {

    public static BusArrivalView found(String stationName, String busNumber, BusArrivalResult arrival) {
        return new BusArrivalView(stationName, busNumber, arrival, null);
    }

    public static BusArrivalView notFound(String stationName, String busNumber) {
        return new BusArrivalView(stationName, busNumber, null, null);
    }

    public static BusArrivalView error(String message) {
        return new BusArrivalView(null, null, null, message);
    }

    public boolean isError() {
        return errorMessage != null;
    }

    public boolean hasArrival() {
        return arrival != null;
    }
}
