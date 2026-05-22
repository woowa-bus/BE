package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import com.woowa.bus.slack.application.SlackMessageSender;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackActionControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void action_delete_calls_alert_service_delete() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = controller(alertService);

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
    void action_delete_replaces_original_message_with_deleted_message_then_remaining_alerts() throws Exception {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = controller(alertService);

        String payload = """
                {
                  "type": "block_actions",
                  "user": {"id": "U123"},
                  "actions": [
                    {"action_id": "alert_delete", "value": "텔레칩스|310"}
                  ]
                }""";

        String body = controller.action(payload).getBody();
        JsonNode blocks = OBJECT_MAPPER.readTree(body).get("blocks");

        assertTrue(body.contains("\"replace_original\":true"));
        assertEquals("context", blocks.get(0).get("type").asText());
        assertTrue(blocks.get(0).get("elements").get(0).get("text").asText()
                .contains("텔레칩스 310번 알림을 삭제했어요."));
        assertEquals("divider", blocks.get(1).get("type").asText());
        assertEquals("header", blocks.get(2).get("type").asText());
        assertTrue(body.contains("🔔 등록된 버스 알림"));
        assertTrue(body.contains("벤처타운(북문)"));
    }

    @Test
    void action_view_submission_creates_alert_closes_modal_and_sends_success_message() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        FakeSlackMessageSender messageSender = new FakeSlackMessageSender();
        SlackActionController controller = controller(alertService, messageSender);

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
        assertEquals("U123", messageSender.lastUserId);
        assertTrue(messageSender.lastText.contains("정류장: 텔레칩스"));
        assertTrue(messageSender.lastText.contains("버스: 310번"));
        assertNull(messageSender.lastBlocksJson);
    }

    @Test
    void action_view_submission_returns_errors_when_bus_invalid() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        alertService.throwOnSave = new RuntimeException("지원하지 않는 버스예요.");
        FakeSlackMessageSender messageSender = new FakeSlackMessageSender();
        SlackActionController controller = controller(alertService, messageSender);

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
        assertNull(messageSender.lastText);
    }

    @Test
    void action_boarded_marks_alert_and_replaces_message() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = controller(alertService);

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
        assertTrue(body.contains("모든 버스 알림이 울리지 않습니다."));
    }

    @Test
    void action_unknown_id_is_ignored() {
        FakeBusAlertService alertService = new FakeBusAlertService();
        SlackActionController controller = controller(alertService);

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

    private SlackActionController controller(FakeBusAlertService alertService) {
        return controller(alertService, new FakeSlackMessageSender());
    }

    private SlackActionController controller(FakeBusAlertService alertService, SlackMessageSender messageSender) {
        return new SlackActionController(alertService, new SlackBlockKitBuilder(), messageSender);
    }

    private static class FakeSlackMessageSender implements SlackMessageSender {

        private String lastUserId;
        private String lastText;
        private String lastBlocksJson;

        @Override
        public void sendDm(String slackUserId, String text, String blocksJson) {
            this.lastUserId = slackUserId;
            this.lastText = text;
            this.lastBlocksJson = blocksJson;
        }
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
            return "🚌 좋은 하루 보내세요! 오늘은 더 이상 모든 버스 알림이 울리지 않습니다.";
        }

        @Override
        public String delete(BusAlertDeleteCommand command) {
            this.lastDeleted = command;
            return "🗑️ %s %s번 알림을 삭제했어요.".formatted(command.stationName(), command.busNumber());
        }

        @Override
        public String save(BusAlertCreateCommand command) {
            if (throwOnSave != null) {
                throw throwOnSave;
            }
            this.lastSaved = command;
            return """
                    ✅ 버스 알림을 등록했어요.

                    정류장: %s
                    버스: %s번""".formatted(command.stationName(), command.busNumber());
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
