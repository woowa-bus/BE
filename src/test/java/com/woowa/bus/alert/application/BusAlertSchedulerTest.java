package com.woowa.bus.alert.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import com.woowa.bus.slack.application.SlackMessageSender;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BusAlertSchedulerTest {

    @Test
    void sendBusAlerts_success() {
        FakeBusAlertRepository repository = new FakeBusAlertRepository();
        repository.save(BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30)));
        FakeSlackMessageSender slackMessageSender = new FakeSlackMessageSender();
        BusAlertScheduler scheduler = new BusAlertScheduler(
                repository,
                registry(),
                new FakeBusArrivalClient(),
                slackMessageSender,
                Clock.fixed(Instant.parse("2026-05-22T09:01:00Z"), ZoneId.of("Asia/Seoul")),
                10
        );

        scheduler.sendBusAlerts();

        assertEquals(1, slackMessageSender.messages.size());
    }

    @Test
    void sendBusAlerts_does_not_send_when_recently_notified() {
        FakeBusAlertRepository repository = new FakeBusAlertRepository();
        BusAlert alert = BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30));
        alert.markNotified(Instant.parse("2026-05-22T08:55:00Z").atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime());
        repository.save(alert);
        FakeSlackMessageSender slackMessageSender = new FakeSlackMessageSender();
        BusAlertScheduler scheduler = new BusAlertScheduler(
                repository,
                registry(),
                new FakeBusArrivalClient(),
                slackMessageSender,
                Clock.fixed(Instant.parse("2026-05-22T09:01:00Z"), ZoneId.of("Asia/Seoul")),
                10
        );

        scheduler.sendBusAlerts();

        assertEquals(0, slackMessageSender.messages.size());
    }

    private BusRouteRegistry registry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                ))
        ));
    }

    private static class FakeBusArrivalClient implements BusArrivalClient {

        @Override
        public BusArrivalResult getArrival(String stationId, String routeId, String staOrder) {
            return new BusArrivalResult("310", 4, 13);
        }
    }

    private static class FakeSlackMessageSender implements SlackMessageSender {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendDm(String slackUserId, String message) {
            messages.add(message);
        }
    }

    private static class FakeBusAlertRepository implements BusAlertRepository {

        private final List<BusAlert> alerts = new ArrayList<>();

        @Override
        public BusAlert save(BusAlert alert) {
            alerts.add(alert);
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
