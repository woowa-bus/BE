package com.woowa.bus.slack.presentation;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.search.application.BusArrivalSearchService;
import com.woowa.bus.message.BusMessageFormatter;
import com.woowa.bus.slack.application.BusCommandHelpService;
import com.woowa.bus.slack.application.BusStatusService;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SlackCommandController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final BusArrivalSearchService busArrivalSearchService;
    private final BusAlertService busAlertService;
    private final BusCommandHelpService busCommandHelpService;
    private final BusStatusService busStatusService;
    private final SlackBlockKitBuilder slackBlockKitBuilder;

    public SlackCommandController(
            BusArrivalSearchService busArrivalSearchService,
            BusAlertService busAlertService,
            BusCommandHelpService busCommandHelpService,
            BusStatusService busStatusService,
            SlackBlockKitBuilder slackBlockKitBuilder
    ) {
        this.busArrivalSearchService = busArrivalSearchService;
        this.busAlertService = busAlertService;
        this.busCommandHelpService = busCommandHelpService;
        this.busStatusService = busStatusService;
        this.slackBlockKitBuilder = slackBlockKitBuilder;
    }

    @PostMapping("/slack/commands/search")
    public ResponseEntity<String> search(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        log.info("Slack search command received. userId={}, rawText={}", slackUserId, text);
        try {
            String[] tokens = tokens(text);
            log.debug("Slack search tokens parsed. userId={}, tokens={}", slackUserId, List.of(tokens));
            if (tokens.length == 1) {
                String body = slackBlockKitBuilder.stationArrival(busArrivalSearchService.resolveStation(tokens[0]));
                log.info("Slack search station completed. userId={}, stationName={}", slackUserId, tokens[0]);
                return jsonOk(body);
            }
            if (tokens.length == 2) {
                String body = slackBlockKitBuilder.busArrival(busArrivalSearchService.resolveBus(tokens[0], tokens[1]));
                log.info("Slack search bus completed. userId={}, stationName={}, busNumber={}", slackUserId, tokens[0], tokens[1]);
                return jsonOk(body);
            }
            return ResponseEntity.ok(searchUsage());
        } catch (RuntimeException exception) {
            log.error("Slack search command failed. userId={}, rawText={}", slackUserId, text, exception);
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    private ResponseEntity<String> jsonOk(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping("/slack/commands/alert")
    public ResponseEntity<String> alert(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        log.info("Slack alert command received. userId={}, rawText={}", slackUserId, text);
        try {
            String[] tokens = tokens(text);
            log.debug("Slack alert tokens parsed. userId={}, tokens={}", slackUserId, List.of(tokens));
            if (tokens.length != 5) {
                return ResponseEntity.ok(alertUsage());
            }
            BusAlertCreateCommand command = new BusAlertCreateCommand(
                    slackUserId,
                    tokens[0],
                    tokens[1],
                    parseNotifyBeforeMinutes(tokens[2]),
                    parseTime(tokens[3]),
                    parseTime(tokens[4])
            );
            String response = busAlertService.save(command);
            log.info("Slack alert command completed. userId={}, stationName={}, busNumber={}", slackUserId, tokens[0], tokens[1]);
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            log.error("Slack alert command failed. userId={}, rawText={}", slackUserId, text, exception);
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    @PostMapping("/slack/commands/alert-list")
    public ResponseEntity<String> alertList(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        List<BusAlertResponse> alerts = busAlertService.findAllBySlackUserId(slackUserId);
        log.info("Slack alert-list command. userId={}, alertCount={}", slackUserId, alerts.size());
        if (alerts.isEmpty()) {
            return ResponseEntity.ok("등록된 버스 알림이 없어요.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(slackBlockKitBuilder.alertList(alerts));
    }

    @PostMapping("/slack/commands/alert-delete")
    public ResponseEntity<String> alertDelete(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        log.info("Slack alert-delete command received. userId={}, rawText={}", slackUserId, text);
        try {
            String[] tokens = tokens(text);
            log.debug("Slack alert-delete tokens parsed. userId={}, tokens={}", slackUserId, List.of(tokens));
            if (tokens.length != 2) {
                return ResponseEntity.ok(BusMessageFormatter.alertDeleteUsage());
            }
            String response = busAlertService.delete(new BusAlertDeleteCommand(slackUserId, tokens[0], tokens[1]));
            log.info("Slack alert-delete command completed. userId={}, stationName={}, busNumber={}", slackUserId, tokens[0], tokens[1]);
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            log.error("Slack alert-delete command failed. userId={}, rawText={}", slackUserId, text, exception);
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    @PostMapping("/slack/commands/help")
    public ResponseEntity<String> help(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        log.info("Slack help command received. userId={}", slackUserId);
        return ResponseEntity.ok(busCommandHelpService.help());
    }

    @PostMapping("/slack/commands/status")
    public ResponseEntity<String> status(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        log.info("Slack status command received. userId={}", slackUserId);
        return ResponseEntity.ok(busStatusService.status());
    }

    private String[] tokens(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return text.trim().split("\\s+");
    }

    private int parseNotifyBeforeMinutes(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("""
                    알림 기준 시간은 1~30분 사이로 입력해 주세요.

                    예시:
                    /알림 텔레칩스 310 5 17:45 23:30""");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("""
                    시간은 HH:mm 형식으로 입력해 주세요.

                    예시:
                    /알림 텔레칩스 310 5 17:45 23:30""");
        }
    }

    private String searchUsage() {
        return BusMessageFormatter.searchUsage();
    }

    private String alertUsage() {
        return BusMessageFormatter.alertUsage();
    }
}
