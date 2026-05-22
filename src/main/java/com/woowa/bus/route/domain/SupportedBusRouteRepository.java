package com.woowa.bus.route.domain;

import java.util.List;

public interface SupportedBusRouteRepository {

    List<SupportedBusStation> findAll();
}
