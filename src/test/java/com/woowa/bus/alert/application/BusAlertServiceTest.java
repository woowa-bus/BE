package com.woowa.bus.alert.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BusAlertServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 5, 22, 18, 1).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private final FakeBusAlertRepository repository = new FakeBusAlertRepository();
    private final BusAlertService service = new BusAlertService(
            repository,
            registry(),
            FIXED_CLOCK
    );

    @Test
    void save_success_when_new_alert() {
        String message = service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        assertTrue(message.startsWith("✅ 버스 알림을 등록했어요."));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void save_success_when_existing_alert_is_updated() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(20, 0)
        ));

        String message = service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                7,
                LocalTime.of(18, 0),
                LocalTime.of(23, 30)
        ));

        BusAlert alert = repository.findByUserStationAndBus("U123", "텔레칩스", "310").orElseThrow();
        assertTrue(message.startsWith("✅ 기존 알림을 업데이트했어요."));
        assertEquals(1, repository.findAll().size());
        assertEquals(7, alert.notifyBeforeMinutes());
    }

    @Test
    void save_success_when_same_user_subscribes_multiple_buses() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "55",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        assertEquals(2, repository.findAll().size());
        assertEquals(2, service.findAllBySlackUserId("U123").size());
    }

    @Test
    void findAllBySlackUserId_success() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        List<BusAlertResponse> responses = service.findAllBySlackUserId("U123");

        assertEquals(1, responses.size());
        assertEquals("텔레칩스", responses.get(0).stationName());
    }

    @Test
    void delete_success_when_alert_exists() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        String message = service.delete(new BusAlertDeleteCommand("U123", "텔레칩스", "310"));

        assertEquals("🗑️ 텔레칩스 310번 알림을 삭제했어요.", message);
        assertFalse(repository.findByUserStationAndBus("U123", "텔레칩스", "310").isPresent());
    }

    @Test
    void markBoarded_success_suppresses_all_user_alerts_today() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "55",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
        service.save(new BusAlertCreateCommand(
                "U999",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));

        String message = service.markBoarded("U123", "텔레칩스", "310");

        assertEquals("""
                오늘 하루 고생하셨어요 내일 봐요~
                오늘 알람은 더이상 울리지 않습니다.""", message);
        List<BusAlert> userAlerts = repository.findAllBySlackUserId("U123");
        assertEquals(2, userAlerts.size());
        assertTrue(userAlerts.stream()
                .noneMatch(alert -> alert.canSendNotification(LocalDateTime.of(2026, 5, 22, 22, 0), 3, 10)));
        assertTrue(userAlerts.stream()
                .allMatch(alert -> alert.canSendNotification(LocalDateTime.of(2026, 5, 23, 18, 1), 3, 10)));
        assertTrue(repository.findByUserStationAndBus("U999", "텔레칩스", "310")
                .orElseThrow()
                .canSendNotification(LocalDateTime.of(2026, 5, 22, 22, 0), 3, 10));
    }

    @Test
    void resetNotifications_success_allows_all_user_alerts_again_today() {
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "310",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
        service.save(new BusAlertCreateCommand(
                "U123",
                "텔레칩스",
                "55",
                5,
                LocalTime.of(17, 45),
                LocalTime.of(23, 30)
        ));
        service.markBoarded("U123", "텔레칩스", "310");

        String message = service.resetNotifications("U123");

        assertEquals("🔔 오늘 알림을 다시 울리도록 초기화했어요.", message);
        assertTrue(repository.findAllBySlackUserId("U123").stream()
                .allMatch(alert -> alert.canSendNotification(LocalDateTime.of(2026, 5, 22, 18, 3), 3, 10)));
    }

    @Test
    void resetNotifications_when_empty_returns_notice() {
        String message = service.resetNotifications("U123");

        assertEquals("초기화할 버스 알림이 없어요.", message);
    }

    private BusRouteRegistry registry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12"),
                        SupportedBusRoute.of("55", "234000002", "13")
                )),
                SupportedBusStation.of("벤처타운(북문)", "200000002", List.of(
                        SupportedBusRoute.of("310", "234000003", "14")
                ))
        ));
    }

    private static class FakeBusAlertRepository implements BusAlertRepository {

        private final List<BusAlert> alerts = new ArrayList<>();

        @Override
        public BusAlert save(BusAlert alert) {
            if (!alerts.contains(alert)) {
                alerts.add(alert);
            }
            return alert;
        }

        @Override
        public List<BusAlert> findAll() {
            return new ArrayList<>(alerts);
        }

        @Override
        public List<BusAlert> findAllBySlackUserId(String slackUserId) {
            return alerts.stream()
                    .filter(alert -> alert.hasSlackUserId(slackUserId))
                    .toList();
        }

        @Override
        public Optional<BusAlert> findByUserStationAndBus(String slackUserId, String stationName, String busNumber) {
            return alerts.stream()
                    .filter(alert -> alert.isSameTarget(slackUserId, stationName, busNumber))
                    .findFirst();
        }

        @Override
        public void delete(BusAlert alert) {
            alerts.remove(alert);
        }
    }
}
