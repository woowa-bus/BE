package com.woowa.bus.alert.infrastructure;

import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBusAlertRepository implements BusAlertRepository {

    private final SpringDataBusAlertRepository springDataBusAlertRepository;

    public JpaBusAlertRepository(SpringDataBusAlertRepository springDataBusAlertRepository) {
        this.springDataBusAlertRepository = springDataBusAlertRepository;
    }

    @Override
    public BusAlert save(BusAlert alert) {
        return springDataBusAlertRepository.save(BusAlertJpaEntity.from(alert)).toDomain();
    }

    @Override
    public List<BusAlert> findAll() {
        return springDataBusAlertRepository.findAll()
                .stream()
                .map(BusAlertJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<BusAlert> findAllBySlackUserId(String slackUserId) {
        return springDataBusAlertRepository.findAllBySlackUserId(slackUserId)
                .stream()
                .map(BusAlertJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<BusAlert> findByUserStationAndBus(String slackUserId, String stationName, String busNumber) {
        return springDataBusAlertRepository.findBySlackUserIdAndStationNameAndBusNumber(
                        slackUserId,
                        stationName,
                        busNumber
                )
                .map(BusAlertJpaEntity::toDomain);
    }

    @Override
    public void delete(BusAlert alert) {
        BusAlertJpaEntity entity = springDataBusAlertRepository.findBySlackUserIdAndStationNameAndBusNumber(
                        alert.slackUserId(),
                        alert.stationName(),
                        alert.busNumber()
                )
                .orElseThrow();
        springDataBusAlertRepository.delete(entity);
    }
}
