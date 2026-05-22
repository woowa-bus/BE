package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.SupportedBusRouteRepository;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class JpaSupportedBusRouteRepository implements SupportedBusRouteRepository {

    private final SpringDataBusStationRepository springDataBusStationRepository;

    public JpaSupportedBusRouteRepository(SpringDataBusStationRepository springDataBusStationRepository) {
        this.springDataBusStationRepository = springDataBusStationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportedBusStation> findAll() {
        List<SupportedBusStation> stations = springDataBusStationRepository.findAllByOrderByNameAsc()
                .stream()
                .map(BusStationJpaEntity::toDomain)
                .toList();
        log.debug("Loaded supported bus stations from DB. count={}", stations.size());
        return stations;
    }
}
