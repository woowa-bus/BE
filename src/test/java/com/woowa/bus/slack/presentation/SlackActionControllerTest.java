package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import java.time.LocalTime;
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
    void action_view_submission_creates_alert_and_closes_modal() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "view_submission",
                  "user": {"id": "U123"},
                  "view": {
                    "callback_id": "alert_create",
                    "state": {
                      "values": {
                        "station": {"station_select": {"selected_option": {"value": "텔레칩스"}}},
                        "bus": {"bus_input": {"value": "310"}},
                        "notify_before": {"notify_before_input": {"value": "5"}},
                        "start_time": {"start_time_input": {"selected_time": "17:45"}},
                        "end_time": {"end_time_input": {"selected_time": "23:30"}}
                      }
                    }
                  }
                }""";

        String body = controller.action(payload).getBody();

        assertEquals("", body);
        assertNotNull(alertService.lastSaved);
        assertEquals("U123", alertService.lastSaved.slackUserId());
        assertEquals("텔레칩스", alertService.lastSaved.stationName());
        assertEquals("310", alertService.lastSaved.busNumber());
        assertEquals(5, alertService.lastSaved.notifyBeforeMinutes());
        assertEquals(LocalTime.of(17, 45), alertService.lastSaved.startTime());
        assertEquals(LocalTime.of(23, 30), alertService.lastSaved.endTime());
    }

    @Test
    void action_view_submission_returns_errors_when_bus_invalid() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        alertService.throwOnSave = new RuntimeException("지원하지 않는 버스예요.");
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "view_submission",
                  "user": {"id": "U123"},
                  "view": {
                    "callback_id": "alert_create",
                    "state": {
                      "values": {
                        "station": {"station_select": {"selected_option": {"value": "텔레칩스"}}},
                        "bus": {"bus_input": {"value": "999"}},
                        "notify_before": {"notify_before_input": {"value": "5"}},
                        "start_time": {"start_time_input": {"selected_time": "17:45"}},
                        "end_time": {"end_time_input": {"selected_time": "23:30"}}
                      }
                    }
                  }
                }""";

        String body = controller.action(payload).getBody();

        assertTrue(body.contains("\"response_action\":\"errors\""));
        assertTrue(body.contains("지원하지 않는 버스"));
    }

    @Test
    void action_boarded_marks_alert_and_replaces_message() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = new SlackActionController(alertService, new SlackBlockKitBuilder());

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "alert_boarded", "value": "텔레칩스|310"}
                  ]
                }""";

        String body = controller.action(payload).getBody();

        assertEquals("U123", alertService.lastBoardedUser);
        assertEquals("텔레칩스", alertService.lastBoardedStation);
        assertEquals("310", alertService.lastBoardedBus);
        assertTrue(body.contains("\"replace_original\":true"));
        assertTrue(body.contains("좋은 하루"));
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
        private BusAlertCreateCommand lastSaved;
        private String lastBoardedUser;
        private String lastBoardedStation;
        private String lastBoardedBus;
        private RuntimeException throwOnSave;

        FakeBusAlertService() {
            super(null, null, null);
        }

        @Override
        public String markBoarded(String slackUserId, String stationName, String busNumber) {
            this.lastBoardedUser = slackUserId;
            this.lastBoardedStation = stationName;
            this.lastBoardedBus = busNumber;
            return "🚌 좋은 하루 보내세요! 오늘은 더 이상 %s %s번 알림을 보내지 않을게요.".formatted(stationName, busNumber);
        }

        @Override
        public String delete(BusAlertDeleteCommand command) {
            this.lastDeleted = command;
            return "deleted";
        }

        @Override
        public String save(BusAlertCreateCommand command) {
            if (throwOnSave != null) {
                throw throwOnSave;
            }
            this.lastSaved = command;
            return "saved";
        }

        @Override
        public List<BusAlertResponse> findAllBySlackUserId(String slackUserId) {
            return List.of(new BusAlertResponse(
                    "벤처타운(북문)", "55", 5,
                    LocalTime.of(17, 45),
                    LocalTime.of(23, 30)
            ));
        }
    }
}
