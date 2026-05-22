package com.woowa.bus.alert.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBusAlertRepository extends JpaRepository<BusAlertJpaEntity, Long> {

    List<BusAlertJpaEntity> findAllBySlackUserId(String slackUserId);

    Optional<BusAlertJpaEntity> findBySlackUserIdAndStationNameAndBusNumber(
            String slackUserId,
            String stationName,
            String busNumber
    );
}
