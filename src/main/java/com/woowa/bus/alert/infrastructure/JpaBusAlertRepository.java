package com.woowa.bus.alert.infrastructure;

import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class JpaBusAlertRepository implements BusAlertRepository {

    private final SpringDataBusAlertRepository springDataBusAlertRepository;

    public JpaBusAlertRepository(SpringDataBusAlertRepository springDataBusAlertRepository) {
        this.springDataBusAlertRepository = springDataBusAlertRepository;
    }

    @Override
    public BusAlert save(BusAlert alert) {
        log.debug("Persisting bus alert. alertId={}, userId={}, stationName={}, busNumber={}",
                alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber());
        return springDataBusAlertRepository.save(BusAlertJpaEntity.from(alert)).toDomain();
    }

    @Override
    public List<BusAlert> findAll() {
        List<BusAlert> alerts = springDataBusAlertRepository.findAll()
                .stream()
                .map(BusAlertJpaEntity::toDomain)
                .toList();
        log.debug("Loaded all bus alerts. count={}", alerts.size());
        return alerts;
    }

    @Override
    public List<BusAlert> findAllBySlackUserId(String slackUserId) {
        List<BusAlert> alerts = springDataBusAlertRepository.findAllBySlackUserId(slackUserId)
                .stream()
                .map(BusAlertJpaEntity::toDomain)
                .toList();
        log.debug("Loaded bus alerts by user. userId={}, count={}", slackUserId, alerts.size());
        return alerts;
    }

    @Override
    public Optional<BusAlert> findByUserStationAndBus(String slackUserId, String stationName, String busNumber) {
        Optional<BusAlert> result = springDataBusAlertRepository.findBySlackUserIdAndStationNameAndBusNumber(
                        slackUserId,
                        stationName,
                        busNumber
                )
                .map(BusAlertJpaEntity::toDomain);
        log.debug("Loaded bus alert by unique key. userId={}, stationName={}, busNumber={}, present={}",
                slackUserId, stationName, busNumber, result.isPresent());
        return result;
    }

    @Override
    public void delete(BusAlert alert) {
        log.debug("Deleting bus alert. alertId={}, userId={}, stationName={}, busNumber={}",
                alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber());
        BusAlertJpaEntity entity = springDataBusAlertRepository.findBySlackUserIdAndStationNameAndBusNumber(
                        alert.slackUserId(),
                        alert.stationName(),
                        alert.busNumber()
                )
                .orElseThrow();
        springDataBusAlertRepository.delete(entity);
    }
}
