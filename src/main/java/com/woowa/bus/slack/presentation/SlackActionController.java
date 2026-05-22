package com.woowa.bus.slack.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BusAlertService busAlertService;
    private final SlackBlockKitBuilder slackBlockKitBuilder;

    public SlackActionController(BusAlertService busAlertService, SlackBlockKitBuilder slackBlockKitBuilder) {
        this.busAlertService = busAlertService;
        this.slackBlockKitBuilder = slackBlockKitBuilder;
    }

    @PostMapping("/slack/actions")
    public ResponseEntity<String> action(@RequestParam("payload") String payload) {
        log.info("Slack action payload received. length={}", payload.length());
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            String userId = root.path("user").path("id").asText();
            JsonNode actions = root.path("actions");
            if (!actions.isArray()) {
                return empty();
            }
            for (JsonNode action : actions) {
                String body = handleAction(userId, action);
                if (body != null) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body);
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

    private String handleAction(String userId, JsonNode action) {
        String actionId = action.path("action_id").asText();
        if (!ACTION_ALERT_DELETE.equals(actionId)) {
            log.warn("Unknown slack action ignored. actionId={}", actionId);
            return null;
        }
        String value = action.path("value").asText();
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("Invalid alert_delete value. value={}", value);
            return null;
        }
        log.info("Slack alert_delete action. userId={}, stationName={}, busNumber={}", userId, parts[0], parts[1]);
        busAlertService.delete(new BusAlertDeleteCommand(userId, parts[0], parts[1]));
        return slackBlockKitBuilder.alertListAfterDelete(busAlertService.findAllBySlackUserId(userId));
    }

    private ResponseEntity<String> empty() {
        return ResponseEntity.ok("");
    }
}
