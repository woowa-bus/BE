package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackActionControllerTest {

    @Test
    void action_delete_calls_alert_service_delete() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "alert_delete", "value": "텔레칩스|310"}
                  ]
                }""";

        controller.action(payload);

        assertNotNull(alertService.lastDeleted);
        assertEquals("U123", alertService.lastDeleted.slackUserId());
        assertEquals("텔레칩스", alertService.lastDeleted.stationName());
        assertEquals("310", alertService.lastDeleted.busNumber());
    }

    @Test
    void action_delete_replaces_original_message_with_remaining_alerts() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "alert_delete", "value": "텔레칩스|310"}
                  ]
                }""";

        String body = controller.action(payload).getBody();

        assertTrue(body.contains("\"replace_original\":true"));
        assertTrue(body.contains("🗑️ 삭제되었습니다."));
        assertTrue(body.contains("벤처타운(북문)"));
    }

    @Test
    void action_unknown_id_is_ignored() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "unknown", "value": "x"}
                  ]
                }""";

        controller.action(payload);

        assertNull(alertService.lastDeleted);
    }

    private static class FakeBusAlertService extends BusAlertService {

        private BusAlertDeleteCommand lastDeleted;

        FakeBusAlertService() {
            super(null, null);
        }

        @Override
        public String delete(BusAlertDeleteCommand command) {
            this.lastDeleted = command;
            return "deleted";
        }

        @Override
        public List<BusAlertResponse> findAllBySlackUserId(String slackUserId) {
            return List.of(new BusAlertResponse(
                    "벤처타운(북문)", "55", 5,
                    java.time.LocalTime.of(17, 45),
                    java.time.LocalTime.of(23, 30)
            ));
        }
    }
}
