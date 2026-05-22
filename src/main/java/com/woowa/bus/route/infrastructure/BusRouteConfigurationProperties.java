package com.woowa.bus.route.infrastructure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bus-routes")
public class BusRouteConfigurationProperties {

    private final List<StationProperty> stations = new ArrayList<>();

    public List<StationProperty> getStations() {
        return stations;
    }

    public static class StationProperty {

        private String name;
        private String stationId;
        private final List<RouteProperty> routes = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStationId() {
            return stationId;
        }

        public void setStationId(String stationId) {
            this.stationId = stationId;
        }

        public List<RouteProperty> getRoutes() {
            return routes;
        }
    }

    public static class RouteProperty {

        private String busNumber;
        private String routeId;
        private String staOrder;

        public String getBusNumber() {
            return busNumber;
        }

        public void setBusNumber(String busNumber) {
            this.busNumber = busNumber;
        }

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        public String getStaOrder() {
            return staOrder;
        }

        public void setStaOrder(String staOrder) {
            this.staOrder = staOrder;
        }
    }
}
