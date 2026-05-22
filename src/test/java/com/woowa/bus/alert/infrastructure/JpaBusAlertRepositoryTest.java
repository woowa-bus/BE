package com.woowa.bus.alert.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.domain.BusAlert;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBusAlertRepository.class)
class JpaBusAlertRepositoryTest {

    @Autowired
    private JpaBusAlertRepository repository;

    @Test
    void save_success() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        repository.save(alert);

        BusAlert found = repository.findByUserStationAndBus("U123", "텔레칩스", "310").orElseThrow();
        assertEquals("텔레칩스", found.stationName());
        assertEquals("310", found.busNumber());
    }

    @Test
    void delete_success() {
        BusAlert alert = repository.save(BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        repository.delete(alert);

        assertTrue(repository.findByUserStationAndBus("U123", "텔레칩스", "310").isEmpty());
    }
}
