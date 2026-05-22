package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bus_stations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bus_stations_station_id", columnNames = "station_id"),
                @UniqueConstraint(name = "uk_bus_stations_name", columnNames = "name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BusStationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String stationId;

    @Column(nullable = false)
    private String mobileNo;

    @Column(nullable = false)
    private String regionName;

    @Column(nullable = false)
    private String centerYn;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @OrderBy("busNumber ASC")
    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<BusRouteJpaEntity> routes = new ArrayList<>();

    private BusStationJpaEntity(
            String name,
            String stationId,
            String mobileNo,
            String regionName,
            String centerYn,
            double x,
            double y
    ) {
        this.name = name;
        this.stationId = stationId;
        this.mobileNo = mobileNo;
        this.regionName = regionName;
        this.centerYn = centerYn;
        this.x = x;
        this.y = y;
    }

    static BusStationJpaEntity of(
            String name,
            String stationId,
            String mobileNo,
            String regionName,
            String centerYn,
            double x,
            double y
    ) {
        return new BusStationJpaEntity(name, stationId, mobileNo, regionName, centerYn, x, y);
    }

    void addRoute(String busNumber, String routeId, String staOrder) {
        if (routes.stream().anyMatch(route -> route.busNumber().equals(busNumber))) {
            return;
        }
        routes.add(BusRouteJpaEntity.of(this, busNumber, routeId, staOrder));
    }

    boolean hasRoute(String busNumber) {
        return routes.stream()
                .anyMatch(route -> route.busNumber().equals(busNumber));
    }

    void syncInfo(
            String name,
            String mobileNo,
            String regionName,
            String centerYn,
            double x,
            double y
    ) {
        this.name = name;
        this.mobileNo = mobileNo;
        this.regionName = regionName;
        this.centerYn = centerYn;
        this.x = x;
        this.y = y;
    }

    SupportedBusStation toDomain() {
        List<SupportedBusRoute> supportedRoutes = routes.stream()
                .map(BusRouteJpaEntity::toDomain)
                .toList();
        return SupportedBusStation.of(name, stationId, supportedRoutes);
    }
}
