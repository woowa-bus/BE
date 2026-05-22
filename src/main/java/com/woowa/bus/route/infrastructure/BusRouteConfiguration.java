package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BusRouteConfigurationProperties.class)
public class BusRouteConfiguration {

    @Bean
    public BusRouteRegistry busRouteRegistry(BusRouteConfigurationProperties properties) {
        List<SupportedBusStation> stations = properties.getStations()
                .stream()
                .map(this::station)
                .toList();
        return BusRouteRegistry.of(stations);
    }

    private SupportedBusStation station(BusRouteConfigurationProperties.StationProperty property) {
        List<SupportedBusRoute> routes = property.getRoutes()
                .stream()
                .map(this::route)
                .toList();
        return SupportedBusStation.of(property.getName(), property.getStationId(), routes);
    }

    private SupportedBusRoute route(BusRouteConfigurationProperties.RouteProperty property) {
        return SupportedBusRoute.of(property.getBusNumber(), property.getRouteId(), property.getStaOrder());
    }
}
