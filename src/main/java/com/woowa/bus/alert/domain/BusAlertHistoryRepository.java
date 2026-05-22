package com.woowa.bus.alert.domain;

import java.util.List;

public interface BusAlertHistoryRepository {

    BusAlertHistory save(BusAlertHistory history);

    List<BusAlertHistory> findAll();
}
