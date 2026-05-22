package com.woowa.bus.slack.presentation;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.search.application.BusArrivalSearchService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SlackCommandController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final BusArrivalSearchService busArrivalSearchService;
    private final BusAlertService busAlertService;

    public SlackCommandController(BusArrivalSearchService busArrivalSearchService, BusAlertService busAlertService) {
        this.busArrivalSearchService = busArrivalSearchService;
        this.busAlertService = busAlertService;
    }

    @PostMapping("/slack/commands/search")
    public ResponseEntity<String> search(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        try {
            String[] tokens = tokens(text);
            if (tokens.length == 1) {
                return ResponseEntity.ok(busArrivalSearchService.searchStation(tokens[0]));
            }
            if (tokens.length == 2) {
                return ResponseEntity.ok(busArrivalSearchService.searchBus(tokens[0], tokens[1]));
            }
            return ResponseEntity.ok(searchUsage());
        } catch (RuntimeException exception) {
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    @PostMapping("/slack/commands/alert")
    public ResponseEntity<String> alert(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        try {
            String[] tokens = tokens(text);
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
            return ResponseEntity.ok(busAlertService.save(command));
        } catch (RuntimeException exception) {
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    @PostMapping("/slack/commands/alert-list")
    public ResponseEntity<String> alertList(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        List<BusAlertResponse> alerts = busAlertService.findAllBySlackUserId(slackUserId);
        if (alerts.isEmpty()) {
            return ResponseEntity.ok("등록된 버스 알림이 없어요.");
        }
        return ResponseEntity.ok(alertListMessage(alerts));
    }

    @PostMapping("/slack/commands/alert-delete")
    public ResponseEntity<String> alertDelete(
            @RequestParam("user_id") String slackUserId,
            @RequestParam(value = "text", defaultValue = "") String text
    ) {
        try {
            String[] tokens = tokens(text);
            if (tokens.length != 2) {
                return ResponseEntity.ok("/알림삭제 [정류장] [버스번호] 형식으로 입력해 주세요.");
            }
            return ResponseEntity.ok(busAlertService.delete(new BusAlertDeleteCommand(slackUserId, tokens[0], tokens[1])));
        } catch (RuntimeException exception) {
            return ResponseEntity.ok(exception.getMessage());
        }
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

    private String alertListMessage(List<BusAlertResponse> alerts) {
        StringBuilder builder = new StringBuilder("🔔 등록된 버스 알림\n\n");
        for (int index = 0; index < alerts.size(); index++) {
            BusAlertResponse alert = alerts.get(index);
            builder.append("%d. %s / %s번 / %d분 전 / %s~%s".formatted(
                    index + 1,
                    alert.stationName(),
                    alert.busNumber(),
                    alert.notifyBeforeMinutes(),
                    alert.startTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    alert.endTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            ));
            if (index < alerts.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private String searchUsage() {
        return """
                /조회 [정류장]
                /조회 [정류장] [버스번호] 형식으로 입력해 주세요.""";
    }

    private String alertUsage() {
        return """
                /알림 [정류장] [버스번호] [몇 분 전] [시작시간] [종료시간] 형식으로 입력해 주세요.

                예시:
                /알림 텔레칩스 310 5 17:45 23:30""";
    }
}
