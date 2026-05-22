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
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import com.woowa.bus.slack.application.SlackMessageSender;
import com.woowa.bus.message.BusMessageFormatter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class BusAlertScheduler {

    private final BusAlertRepository busAlertRepository;
    private final BusAlertHistoryRepository busAlertHistoryRepository;
    private final BusRouteRegistry busRouteRegistry;
    private final BusArrivalClient busArrivalClient;
    private final SlackMessageSender slackMessageSender;
    private final SlackBlockKitBuilder slackBlockKitBuilder;
    private final Clock clock;
    private final int cooldownMinutes;

    public BusAlertScheduler(
            BusAlertRepository busAlertRepository,
            BusAlertHistoryRepository busAlertHistoryRepository,
            BusRouteRegistry busRouteRegistry,
            BusArrivalClient busArrivalClient,
            SlackMessageSender slackMessageSender,
            SlackBlockKitBuilder slackBlockKitBuilder,
            Clock clock,
            @Value("${app.alert.cooldown-minutes:10}") int cooldownMinutes
    ) {
        this.busAlertRepository = busAlertRepository;
        this.busAlertHistoryRepository = busAlertHistoryRepository;
        this.busRouteRegistry = busRouteRegistry;
        this.busArrivalClient = busArrivalClient;
        this.slackMessageSender = slackMessageSender;
        this.slackBlockKitBuilder = slackBlockKitBuilder;
        this.clock = clock;
        this.cooldownMinutes = cooldownMinutes;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendBusAlerts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<BusAlert> alerts = busAlertRepository.findAll();
        log.info("Bus alert scheduler started. now={}, alertCount={}", now, alerts.size());
        Map<String, List<BusAlert>> alertsByStation = alerts.stream()
                .collect(Collectors.groupingBy(alert -> busRouteRegistry.station(alert.stationName()).name()));
        alertsByStation.forEach((stationName, stationAlerts) -> sendBusAlerts(stationName, stationAlerts, now));
        log.info("Bus alert scheduler finished. now={}", now);
    }

    private void sendBusAlerts(String stationName, List<BusAlert> alerts, LocalDateTime now) {
        SupportedBusStation station = busRouteRegistry.station(stationName);
        List<BusArrivalResult> arrivals = busArrivalClient.getArrivals(station.stationId());
        log.debug("Loaded station arrivals for alerts. stationName={}, stationId={}, arrivalCount={}, alertCount={}",
                station.name(), station.stationId(), arrivals.size(), alerts.size());
        alerts.forEach(alert -> sendBusAlert(alert, station, arrivals, now));
    }

    private void sendBusAlert(BusAlert alert, SupportedBusStation station, List<BusArrivalResult> arrivals, LocalDateTime now) {
        log.debug("Evaluating bus alert. alertId={}, userId={}, stationName={}, busNumber={}, lastNotifiedAt={}",
                alert.id(), alert.slackUserId(), alert.stationName(), alert.busNumber(), alert.lastNotifiedAt());
        try {
            BusArrivalResult arrival = findArrival(arrivals, alert.busNumber(), station.stationId());
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
            slackMessageSender.sendDm(
                    alert.slackUserId(),
                    BusMessageFormatter.alertNotification(alert, arrival, now),
                    slackBlockKitBuilder.alertNotificationBlocks(alert, arrival, now)
            );
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

    private BusArrivalResult findArrival(List<BusArrivalResult> arrivals, String busNumber, String stationId) {
        log.debug("Finding alert arrival by bus number. stationId={}, busNumber={}", stationId, busNumber);
        return arrivals.stream()
                .peek(arrival -> log.debug("Alert arrival candidate. stationId={}, busNumber={}, predictTime1={}, predictTime2={}",
                        stationId, arrival.busNumber(), arrival.predictTime1(), arrival.predictTime2()))
                .filter(arrival -> busNumber.equals(arrival.busNumber()))
                .findFirst()
                .orElse(null);
    }
}
