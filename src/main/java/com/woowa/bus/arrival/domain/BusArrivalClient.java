package com.woowa.bus.arrival.domain;

public interface BusArrivalClient {

    BusArrivalResult getArrival(String stationId, String routeId, String staOrder);
}
