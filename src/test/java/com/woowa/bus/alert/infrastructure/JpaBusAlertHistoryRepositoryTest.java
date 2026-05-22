package com.woowa.bus.alert.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.woowa.bus.alert.domain.BusAlertHistory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBusAlertHistoryRepository.class)
class JpaBusAlertHistoryRepositoryTest {

    @Autowired
    private JpaBusAlertHistoryRepository repository;

    @Test
    void save_assigns_id() {
        BusAlertHistory history = BusAlertHistory.record(
                "U123",
                "텔레칩스",
                "310",
                4,
                13,
                LocalDateTime.of(2026, 5, 22, 18, 1)
        );

        BusAlertHistory saved = repository.save(history);

        assertNotNull(saved.id());
        assertEquals("U123", saved.slackUserId());
        assertEquals("310", saved.busNumber());
    }

    @Test
    void findAll_returns_persisted_histories() {
        repository.save(BusAlertHistory.record(
                "U123",
                "텔레칩스",
                "310",
                4,
                13,
                LocalDateTime.of(2026, 5, 22, 18, 1)
        ));
        repository.save(BusAlertHistory.record(
                "U456",
                "벤처타운(북문)",
                "310",
                2,
                null,
                LocalDateTime.of(2026, 5, 22, 18, 5)
        ));

        assertEquals(2, repository.findAll().size());
    }
}
