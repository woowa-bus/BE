package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.SupportedBusRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bus_routes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bus_routes_station_bus",
                        columnNames = {"bus_station_id", "bus_number"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BusRouteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_station_id", nullable = false)
    private BusStationJpaEntity station;

    @Column(nullable = false)
    private String busNumber;

    @Column(nullable = false)
    private String routeId;

    @Column(nullable = false)
    private String staOrder;

    private BusRouteJpaEntity(BusStationJpaEntity station, String busNumber, String routeId, String staOrder) {
        this.station = station;
        this.busNumber = busNumber;
        this.routeId = routeId;
        this.staOrder = staOrder;
    }

    static BusRouteJpaEntity of(BusStationJpaEntity station, String busNumber, String routeId, String staOrder) {
        return new BusRouteJpaEntity(station, busNumber, routeId, staOrder);
    }

    String busNumber() {
        return busNumber;
    }

    SupportedBusRoute toDomain() {
        return SupportedBusRoute.of(busNumber, routeId, staOrder);
    }
}
