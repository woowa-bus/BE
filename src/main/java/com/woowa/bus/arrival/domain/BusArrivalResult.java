package com.woowa.bus.arrival.domain;

public record BusArrivalResult(
        String busNumber,
        Integer predictTime1,
        Integer predictTime2
) {

    public boolean hasArrival() {
        return predictTime1 != null || predictTime2 != null;
    }
}
