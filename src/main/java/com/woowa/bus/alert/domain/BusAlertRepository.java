package com.woowa.bus.alert.domain;

import java.util.List;
import java.util.Optional;

public interface BusAlertRepository {

    BusAlert save(BusAlert alert);

    List<BusAlert> findAll();

    List<BusAlert> findAllBySlackUserId(String slackUserId);

    Optional<BusAlert> findByUserStationAndBus(String slackUserId, String stationName, String busNumber);

    void delete(BusAlert alert);
}
