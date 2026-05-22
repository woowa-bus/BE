package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.SupportedBusRoute;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
class BusRouteDataInitializer implements ApplicationRunner {

    private final SpringDataBusStationRepository repository;
    private final GbisBusStationRouteClient gbisBusStationRouteClient;

    BusRouteDataInitializer(
            SpringDataBusStationRepository repository,
            GbisBusStationRouteClient gbisBusStationRouteClient
    ) {
        this.repository = repository;
        this.gbisBusStationRouteClient = gbisBusStationRouteClient;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing default bus station data.");
        BusStationJpaEntity telechips = saveIfAbsent(BusStationJpaEntity.of(
                "텔레칩스",
                "204000158",
                "05341",
                "성남",
                "N",
                127.0873667,
                37.40645
        ));
        BusStationJpaEntity ventureTown = saveIfAbsent(BusStationJpaEntity.of(
                "벤처타운(북문)",
                "204000159",
                "05342",
                "성남",
                "N",
                127.0882833,
                37.4064167
        ));
        seedRoutes(telechips);
        seedRoutes(ventureTown);
    }

    private BusStationJpaEntity saveIfAbsent(BusStationJpaEntity station) {
        return repository.findByStationId(station.getStationId())
                .map(existing -> {
                    existing.syncInfo(
                            station.getName(),
                            station.getMobileNo(),
                            station.getRegionName(),
                            station.getCenterYn(),
                            station.getX(),
                            station.getY()
                    );
                    repository.save(existing);
                    log.info("Bus station already exists. stationId={}, name={}", existing.getStationId(), existing.getName());
                    return existing;
                })
                .orElseGet(() -> {
                    BusStationJpaEntity saved = repository.save(station);
                    log.info("Bus station seeded. stationId={}, name={}", saved.getStationId(), saved.getName());
                    return saved;
                });
    }

    private void seedRoutes(BusStationJpaEntity station) {
        List<SupportedBusRoute> routes = gbisBusStationRouteClient.getRoutes(station.getStationId());
        if (routes.isEmpty()) {
            log.warn("No bus routes returned for station. stationId={}, name={}", station.getStationId(), station.getName());
            return;
        }
        int addedCount = 0;
        for (SupportedBusRoute route : routes) {
            if (station.hasRoute(route.busNumber())) {
                continue;
            }
            station.addRoute(route.busNumber(), route.routeId(), route.staOrder());
            addedCount++;
        }
        if (addedCount > 0) {
            repository.save(station);
        }
        log.info("Bus routes synced. stationId={}, name={}, fetchedCount={}, addedCount={}",
                station.getStationId(), station.getName(), routes.size(), addedCount);
    }
}
