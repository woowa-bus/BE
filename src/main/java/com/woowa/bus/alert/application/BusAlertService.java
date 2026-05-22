package com.woowa.bus.alert.application;

import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.alert.domain.BusAlertRepository;
import com.woowa.bus.route.domain.BusRouteRegistry;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class BusAlertService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BusAlertRepository busAlertRepository;
    private final BusRouteRegistry busRouteRegistry;

    public BusAlertService(BusAlertRepository busAlertRepository, BusRouteRegistry busRouteRegistry) {
        this.busAlertRepository = busAlertRepository;
        this.busRouteRegistry = busRouteRegistry;
    }

    public String save(BusAlertCreateCommand command) {
        log.info("Saving bus alert. userId={}, stationName={}, busNumber={}, notifyBeforeMinutes={}, startTime={}, endTime={}",
                command.slackUserId(), command.stationName(), command.busNumber(),
                command.notifyBeforeMinutes(), command.startTime(), command.endTime());
        busRouteRegistry.route(command.stationName(), command.busNumber());
        return busAlertRepository.findByUserStationAndBus(
                        command.slackUserId(),
                        command.stationName(),
                        command.busNumber()
                )
                .map(alert -> update(alert, command))
                .orElseGet(() -> create(command));
    }

    @Transactional(readOnly = true)
    public List<BusAlertResponse> findAllBySlackUserId(String slackUserId) {
        log.info("Loading bus alerts for user. userId={}", slackUserId);
        return busAlertRepository.findAllBySlackUserId(slackUserId)
                .stream()
                .map(BusAlertResponse::from)
                .toList();
    }

    public String delete(BusAlertDeleteCommand command) {
        log.info("Deleting bus alert. userId={}, stationName={}, busNumber={}",
                command.slackUserId(), command.stationName(), command.busNumber());
        busRouteRegistry.route(command.stationName(), command.busNumber());
        return busAlertRepository.findByUserStationAndBus(
                        command.slackUserId(),
                        command.stationName(),
                        command.busNumber()
                )
                .map(this::delete)
                .orElse("""
                        삭제할 알림을 찾지 못했어요.

                        등록된 알림은 /알림목록 으로 확인할 수 있어요.""");
    }

    private String create(BusAlertCreateCommand command) {
        BusAlert alert = BusAlert.create(
                command.slackUserId(),
                command.stationName(),
                command.busNumber(),
                command.notifyBeforeMinutes(),
                command.startTime(),
                command.endTime()
        );
        busAlertRepository.save(alert);
        log.info("Bus alert created. userId={}, stationName={}, busNumber={}", command.slackUserId(), command.stationName(), command.busNumber());
        return successMessage("✅ 버스 알림을 등록했어요.", command);
    }

    private String update(BusAlert alert, BusAlertCreateCommand command) {
        alert.updateNotificationRule(command.notifyBeforeMinutes(), command.startTime(), command.endTime());
        busAlertRepository.save(alert);
        log.info("Bus alert updated. userId={}, stationName={}, busNumber={}", command.slackUserId(), command.stationName(), command.busNumber());
        return successMessage("✅ 기존 알림을 업데이트했어요.", command);
    }

    private String delete(BusAlert alert) {
        busAlertRepository.delete(alert);
        log.info("Bus alert deleted. userId={}, stationName={}, busNumber={}", alert.slackUserId(), alert.stationName(), alert.busNumber());
        return "🗑️ %s %s번 알림을 삭제했어요.".formatted(alert.stationName(), alert.busNumber());
    }

    private String successMessage(String title, BusAlertCreateCommand command) {
        return """
                %s

                정류장: %s
                버스: %s번
                알림 기준: 도착 %d분 전
                알림 시간: %s~%s
                알림 방식: DM""".formatted(
                title,
                command.stationName(),
                command.busNumber(),
                command.notifyBeforeMinutes(),
                command.startTime().format(TIME_FORMATTER),
                command.endTime().format(TIME_FORMATTER)
        );
    }
}
