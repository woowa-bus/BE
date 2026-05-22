package com.woowa.bus.alert.application;

import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertHistory;
import com.woowa.bus.alert.domain.BusAlertHistoryRepository;
import com.woowa.bus.alert.domain.BusAlertRepository;
import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.route.domain.BusRouteException;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusStation;
import com.woowa.bus.slack.application.SlackMessageSender;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class BusAlertScheduler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BusAlertRepository busAlertRepository;
    private final BusAlertHistoryRepository busAlertHistoryRepository;
    private final BusRouteRegistry busRouteRegistry;
    private final BusArrivalClient busArrivalClient;
    private final SlackMessageSender slackMessageSender;
    private final Clock clock;
    private final int cooldownMinutes;

    public BusAlertScheduler(
            BusAlertRepository busAlertRepository,
            BusAlertHistoryRepository busAlertHistoryRepository,
            BusRouteRegistry busRouteRegistry,
            BusArrivalClient busArrivalClient,
            SlackMessageSender slackMessageSender,
            Clock clock,
            @Value("${app.alert.cooldown-minutes:10}") int cooldownMinutes
    ) {
        this.busAlertRepository = busAlertRepository;
        this.busAlertHistoryRepository = busAlertHistoryRepository;
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
        List<BusAlert> alerts = busAlertRepository.findAll();
        log.info("Bus alert scheduler started. now={}, alertCount={}", now, alerts.size());
        alerts.forEach(alert -> sendBusAlert(alert, now));
        log.info("Bus alert scheduler finished. now={}", now);
    }

    private void sendBusAlert(BusAlert alert, LocalDateTime now) {
        log.debug("Evaluating bus alert. alertId={}, userId={}, stationName={}, busNumber={}, lastNotifiedAt={}",
                alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), alert.lastNotifiedAt());
        try {
            SupportedBusStation station = busRouteRegistry.station(alert.stationName());
            BusArrivalResult arrival = findArrival(station.stationId(), alert.busNumber());
            if (arrival == null) {
                log.warn("No arrival info for alert. alertId={}, userId={}, stationName={}, busNumber={}",
                        alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber());
                return;
            }
            if (!alert.canSendNotification(now, arrival.predictTime1(), cooldownMinutes)) {
                log.debug("Bus alert skipped. alertId={}, userId={}, stationName={}, busNumber={}, predictTime1={}, cooldownMinutes={}",
                        alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), arrival.predictTime1(), cooldownMinutes);
                return;
            }
            log.info("Sending bus alert DM. alertId={}, userId={}, stationName={}, busNumber={}, predictTime1={}",
                    alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), arrival.predictTime1());
            slackMessageSender.sendDm(alert.slackUserId(), message(alert, arrival));
            alert.markNotified(now);
            busAlertRepository.save(alert);
            busAlertHistoryRepository.save(BusAlertHistory.record(
                    alert.slackUserId(),
                    alert.stationName(),
                    alert.busNumber(),
                    arrival.predictTime1(),
                    arrival.predictTime2(),
                    now
            ));
            log.info("Bus alert marked as notified. alertId={}, userId={}, stationName={}, busNumber={}",
                    alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber());
        } catch (BusRouteException exception) {
            log.error("Bus alert route lookup failed. alertId={}, userId={}, stationName={}, busNumber={}",
                    alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), exception);
        } catch (BusArrivalException exception) {
            log.warn("Bus alert arrival lookup failed. alertId={}, userId={}, stationName={}, busNumber={}",
                    alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), exception);
        } catch (RuntimeException exception) {
            log.error("Bus alert send failed unexpectedly. alertId={}, userId={}, stationName={}, busNumber={}",
                    alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), exception);
        }
    }

    private BusArrivalResult findArrival(String stationId, String busNumber) {
        log.debug("Finding alert arrival by bus number. stationId={}, busNumber={}", stationId, busNumber);
        return busArrivalClient.getArrivals(stationId)
                .stream()
                .peek(arrival -> log.debug("Alert arrival candidate. stationId={}, busNumber={}, predictTime1={}, predictTime2={}",
                        stationId, arrival.busNumber(), arrival.predictTime1(), arrival.predictTime2()))
                .filter(arrival -> busNumber.equals(arrival.busNumber()))
                .findFirst()
                .orElse(null);
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
