package com.woowa.bus.alert.application;

import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import com.woowa.bus.slack.application.SlackMessageSender;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BusAlertScheduler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BusAlertRepository busAlertRepository;
    private final BusRouteRegistry busRouteRegistry;
    private final BusArrivalClient busArrivalClient;
    private final SlackMessageSender slackMessageSender;
    private final Clock clock;
    private final int cooldownMinutes;

    public BusAlertScheduler(
            BusAlertRepository busAlertRepository,
            BusRouteRegistry busRouteRegistry,
            BusArrivalClient busArrivalClient,
            SlackMessageSender slackMessageSender,
            Clock clock,
            @Value("${app.alert.cooldown-minutes:10}") int cooldownMinutes
    ) {
        this.busAlertRepository = busAlertRepository;
        this.busRouteRegistry = busRouteRegistry;
        this.busArrivalClient = busArrivalClient;
        this.slackMessageSender = slackMessageSender;
        this.clock = clock;
        this.cooldownMinutes = cooldownMinutes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendBusAlerts() {
        LocalDateTime now = LocalDateTime.now(clock);
        busAlertRepository.findAll()
                .forEach(alert -> sendBusAlert(alert, now));
    }

    private void sendBusAlert(BusAlert alert, LocalDateTime now) {
        SupportedBusStation station = busRouteRegistry.station(alert.stationName());
        SupportedBusRoute route = station.route(alert.busNumber());
        BusArrivalResult arrival = busArrivalClient.getArrival(station.stationId(), route.routeId(), route.staOrder());
        if (!alert.canSendNotification(now, arrival.predictTime1(), cooldownMinutes)) {
            return;
        }
        slackMessageSender.sendDm(alert.slackUserId(), message(alert, arrival));
        alert.markNotified(now);
        busAlertRepository.save(alert);
    }

    private String message(BusAlert alert, BusArrivalResult arrival) {
        return """
                🔔 %s번 버스가 곧 도착해요!

                정류장: %s
                예상 도착: %s
                다음 버스: %s
                알림 기준: %d분 전
                알림 시간: %s~%s""".formatted(
                alert.busNumber(),
                alert.stationName(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2()),
                alert.notifyBeforeMinutes(),
                alert.startTime().format(TIME_FORMATTER),
                alert.endTime().format(TIME_FORMATTER)
        );
    }

    private String arrivalText(Integer predictTime) {
        if (predictTime == null) {
            return "정보 없음";
        }
        return "%d분 후".formatted(predictTime);
    }
}
