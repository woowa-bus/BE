package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import org.junit.jupiter.api.Test;

class SlackActionControllerTest {

    @Test
    void action_delete_calls_alert_service_delete() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService);

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
    void action_unknown_id_is_ignored() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService);

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "unknown", "value": "x"}
                  ]
                }""";

        controller.action(payload);

        assertEquals(null, alertService.lastDeleted);
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
    }
}
