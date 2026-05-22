package com.woowa.bus.slack.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import com.woowa.bus.slack.application.SlackMessageSender;
import com.woowa.bus.slack.application.SlackModalBuilder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SlackActionController {

    private static final String ACTION_ALERT_DELETE = "alert_delete";
    private static final String ACTION_ALERT_BOARDED = "alert_boarded";
    private static final String TYPE_VIEW_SUBMISSION = "view_submission";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BusAlertService busAlertService;
    private final SlackBlockKitBuilder slackBlockKitBuilder;
    private final SlackMessageSender slackMessageSender;

    public SlackActionController(
            BusAlertService busAlertService,
            SlackBlockKitBuilder slackBlockKitBuilder,
            SlackMessageSender slackMessageSender
    ) {
        this.busAlertService = busAlertService;
        this.slackBlockKitBuilder = slackBlockKitBuilder;
        this.slackMessageSender = slackMessageSender;
    }

    @PostMapping("/slack/actions")
    public ResponseEntity<String> action(@RequestParam("payload") String payload) {
        log.info("Slack action payload received. length={}", payload.length());
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            String type = root.path("type").asText();
            String userId = root.path("user").path("id").asText();

            if (TYPE_VIEW_SUBMISSION.equals(type)) {
                return handleViewSubmission(userId, root.path("view"));
            }

            String responseUrl = root.path("response_url").asText();
            JsonNode actions = root.path("actions");
            if (!actions.isArray()) {
                return empty();
            }
            for (JsonNode action : actions) {
                String body = handleAction(userId, action);
                if (body != null) {
                    if (responseUrl != null && !responseUrl.isBlank()) {
                        slackMessageSender.respond(responseUrl, body);
                        return empty();
                    }
                    return jsonOk(body);
                }
            }
            return empty();
        } catch (RuntimeException exception) {
            log.error("Slack action handling failed.", exception);
            return empty();
        } catch (Exception exception) {
            log.error("Slack action payload parse failed.", exception);
            return empty();
        }
    }

    private ResponseEntity<String> handleViewSubmission(String userId, JsonNode view) {
        String callbackId = view.path("callback_id").asText();
        if (!SlackModalBuilder.CALLBACK_ALERT_CREATE.equals(callbackId)) {
            log.warn("Unknown view_submission callback ignored. callbackId={}", callbackId);
            return empty();
        }
        JsonNode values = view.path("state").path("values");
        String station = values.path(SlackModalBuilder.BLOCK_STATION)
                .path(SlackModalBuilder.ACTION_STATION)
                .path("selected_option").path("value").asText();
        String bus = values.path(SlackModalBuilder.BLOCK_BUS)
                .path(SlackModalBuilder.ACTION_BUS)
                .path("value").asText().trim();
        String notifyBeforeText = values.path(SlackModalBuilder.BLOCK_NOTIFY_BEFORE)
                .path(SlackModalBuilder.ACTION_NOTIFY_BEFORE)
                .path("value").asText();
        String startTimeText = values.path(SlackModalBuilder.BLOCK_START_TIME)
                .path(SlackModalBuilder.ACTION_START_TIME)
                .path("selected_time").asText();
        String endTimeText = values.path(SlackModalBuilder.BLOCK_END_TIME)
                .path(SlackModalBuilder.ACTION_END_TIME)
                .path("selected_time").asText();

        try {
            int notifyBefore = Integer.parseInt(notifyBeforeText);
            LocalTime startTime = LocalTime.parse(startTimeText, TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(endTimeText, TIME_FORMATTER);
            String message = busAlertService.save(new BusAlertCreateCommand(userId, station, bus, notifyBefore, startTime, endTime));
            sendAlertCreatedMessage(userId, message);
            log.info("Slack alert modal submission accepted. userId={}, station={}, bus={}", userId, station, bus);
            return empty();
        } catch (NumberFormatException exception) {
            return submissionError(SlackModalBuilder.BLOCK_NOTIFY_BEFORE, "1~30 사이 숫자를 입력해 주세요.");
        } catch (DateTimeParseException exception) {
            return submissionError(SlackModalBuilder.BLOCK_START_TIME, "시간을 선택해 주세요.");
        } catch (RuntimeException exception) {
            log.warn("Slack alert modal submission rejected. userId={}, station={}, bus={}, reason={}",
                    userId, station, bus, exception.getMessage());
            return submissionError(SlackModalBuilder.BLOCK_BUS, exception.getMessage());
        }
    }

    private void sendAlertCreatedMessage(String userId, String message) {
        try {
            slackMessageSender.sendDm(userId, message, null);
        } catch (RuntimeException exception) {
            log.warn("Slack alert modal success message send failed. userId={}", userId, exception);
        }
    }

    private ResponseEntity<String> submissionError(String blockId, String message) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_action", "errors");
        ObjectNode errors = root.putObject("errors");
        errors.put(blockId, message);
        return jsonOk(root.toString());
    }

    private ResponseEntity<String> jsonOk(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String handleAction(String userId, JsonNode action) {
        String actionId = action.path("action_id").asText();
        String value = action.path("value").asText();
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("Invalid slack action value. actionId={}, value={}", actionId, value);
            return null;
        }
        return switch (actionId) {
            case ACTION_ALERT_DELETE -> {
                log.info("Slack alert_delete action. userId={}, stationName={}, busNumber={}", userId, parts[0], parts[1]);
                String deletedMessage = busAlertService.delete(new BusAlertDeleteCommand(userId, parts[0], parts[1]));
                yield slackBlockKitBuilder.alertListAfterDelete(busAlertService.findAllBySlackUserId(userId), deletedMessage);
            }
            case ACTION_ALERT_BOARDED -> {
                log.info("Slack alert_boarded action. userId={}, stationName={}, busNumber={}", userId, parts[0], parts[1]);
                String message = busAlertService.markBoarded(userId, parts[0], parts[1]);
                yield slackBlockKitBuilder.boardedAcknowledgement(message);
            }
            default -> {
                log.warn("Unknown slack action ignored. actionId={}", actionId);
                yield null;
            }
        };
    }

    private ResponseEntity<String> empty() {
        return ResponseEntity.ok("");
    }
}
