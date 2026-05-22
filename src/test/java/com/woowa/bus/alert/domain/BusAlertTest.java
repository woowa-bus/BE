package com.woowa.bus.alert.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class BusAlertTest {

    @Test
    void create_success() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        assertEquals("텔레칩스", alert.stationName());
        assertEquals("310", alert.busNumber());
        assertEquals(5, alert.notifyBeforeMinutes());
    }

    @Test
    void create_fail_with_notify_before_minutes_out_of_range() {
        assertThrows(BusAlertException.class, () -> BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                0,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
    }

    @Test
    void create_fail_when_end_time_is_not_after_start_time() {
        assertThrows(BusAlertException.class, () -> BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(23, 0),
                LocalTime.of(1, 0)
        ));
    }

    @Test
    void update_success() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(20, 0)
        );

        alert.updateNotificationRule(7, LocalTime.of(18, 0), LocalTime.of(23, 30));

        assertEquals(7, alert.notifyBeforeMinutes());
        assertEquals(LocalTime.of(18, 0), alert.startTime());
        assertEquals(LocalTime.of(23, 30), alert.endTime());
    }

    @Test
    void canSendNotification_success_when_time_arrival_and_cooldown_are_satisfied() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        boolean result = alert.canSendNotification(
                LocalDateTime.of(2026, 5, 22, 18, 1),
                4,
                10
        );

        assertTrue(result);
    }

    @Test
    void canSendNotification_fail_when_recently_notified() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );
        alert.markNotified(LocalDateTime.of(2026, 5, 22, 18, 1));

        boolean result = alert.canSendNotification(
                LocalDateTime.of(2026, 5, 22, 18, 10),
                4,
                10
        );

        assertFalse(result);
    }

    @Test
    void markBoardedToday_suppresses_further_alerts_today() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        alert.markBoardedToday(LocalDateTime.of(2026, 5, 22, 18, 1));

        assertFalse(alert.canSendNotification(LocalDateTime.of(2026, 5, 22, 22, 0), 3, 10));
    }

    @Test
    void markBoardedToday_does_not_block_next_day() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        alert.markBoardedToday(LocalDateTime.of(2026, 5, 22, 18, 1));

        assertTrue(alert.canSendNotification(LocalDateTime.of(2026, 5, 23, 18, 1), 3, 10));
    }

    @Test
    void resetNotification_allows_alert_again_after_boarded_today() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );
        LocalDateTime now = LocalDateTime.of(2026, 5, 22, 18, 1);
        alert.markBoardedToday(now);

        alert.resetNotification(now.plusMinutes(1));

        assertTrue(alert.canSendNotification(LocalDateTime.of(2026, 5, 22, 18, 3), 3, 10));
    }

    @Test
    void canSendNotification_handles_no_arrival_information() {
        BusAlert alert = BusAlert.create(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        );

        assertDoesNotThrow(() -> alert.canSendNotification(
                LocalDateTime.of(2026, 5, 22, 18, 1),
                null,
                10
        ));
        assertFalse(alert.canSendNotification(LocalDateTime.of(2026, 5, 22, 18, 1), null, 10));
    }
}
