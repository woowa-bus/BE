package com.woowa.bus.slack.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import lombok.extern.slf4j.Slf4j;
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

    public SlackActionController(BusAlertService busAlertService) {
        this.busAlertService = busAlertService;
    }

    @PostMapping("/slack/actions")
    public ResponseEntity<String> action(@RequestParam("payload") String payload) {
        log.info("Slack action payload received. length={}", payload.length());
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            String userId = root.path("user").path("id").asText();
            JsonNode actions = root.path("actions");
            if (!actions.isArray()) {
                return ResponseEntity.ok("");
            }
            for (JsonNode action : actions) {
                handleAction(userId, action);
            }
            return ResponseEntity.ok("");
        } catch (RuntimeException exception) {
            log.error("Slack action handling failed.", exception);
            return ResponseEntity.ok("");
        } catch (Exception exception) {
            log.error("Slack action payload parse failed.", exception);
            return ResponseEntity.ok("");
        }
    }

    private void handleAction(String userId, JsonNode action) {
        String actionId = action.path("action_id").asText();
        if (!ACTION_ALERT_DELETE.equals(actionId)) {
            log.warn("Unknown slack action ignored. actionId={}", actionId);
            return;
        }
        String value = action.path("value").asText();
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("Invalid alert_delete value. value={}", value);
            return;
        }
        log.info("Slack alert_delete action. userId={}, stationName={}, busNumber={}", userId, parts[0], parts[1]);
        busAlertService.delete(new BusAlertDeleteCommand(userId, parts[0], parts[1]));
    }
}
