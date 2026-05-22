package com.woowa.bus.alert.infrastructure;

import com.woowa.bus.alert.domain.BusAlertHistory;
import com.woowa.bus.alert.domain.BusAlertHistoryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class JpaBusAlertHistoryRepository implements BusAlertHistoryRepository {

    private final SpringDataBusAlertHistoryRepository springDataBusAlertHistoryRepository;

    public JpaBusAlertHistoryRepository(SpringDataBusAlertHistoryRepository springDataBusAlertHistoryRepository) {
        this.springDataBusAlertHistoryRepository = springDataBusAlertHistoryRepository;
    }

    @Override
    public BusAlertHistory save(BusAlertHistory history) {
        log.debug("Persisting bus alert history. userId={}, stationName={}, busNumber={}, notifiedAt={}",
                history.slackUserId(), history.stationName(), history.busNumber(), history.notifiedAt());
        return springDataBusAlertHistoryRepository.save(BusAlertHistoryJpaEntity.from(history)).toDomain();
    }

    @Override
    public List<BusAlertHistory> findAll() {
        List<BusAlertHistory> histories = springDataBusAlertHistoryRepository.findAll()
                .stream()
                .map(BusAlertHistoryJpaEntity::toDomain)
                .toList();
        log.debug("Loaded all bus alert histories. count={}", histories.size());
        return histories;
    }
}
