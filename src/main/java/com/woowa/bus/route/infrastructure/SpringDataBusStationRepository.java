package com.woowa.bus.route.infrastructure;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBusStationRepository extends JpaRepository<BusStationJpaEntity, Long> {

    boolean existsByStationId(String stationId);

    Optional<BusStationJpaEntity> findByStationId(String stationId);

    List<BusStationJpaEntity> findAllByOrderByNameAsc();
}
