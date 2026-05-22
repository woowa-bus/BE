package com.woowa.bus.slack.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.search.application.BusArrivalView;
import com.woowa.bus.search.application.StationArrivalView;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackBlockKitBuilderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SlackBlockKitBuilder builder = new SlackBlockKitBuilder();

    @Test
    void alertList_renders_section_and_delete_button_per_alert() throws Exception {
        String json = builder.alertList(List.of(
                BusAlertResponse.from(BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30))),
                BusAlertResponse.from(BusAlert.create("U123", "벤처타운(북문)", "55", 3, LocalTime.of(8, 0), LocalTime.of(9, 30)))
        ));

        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode blocks = root.get("blocks");

        assertEquals("ephemeral", root.get("response_type").asText());
        assertTrue(blocks.isArray());

        long alertSections = blocks.findValues("accessory").stream()
                .filter(accessory -> "button".equals(accessory.get("type").asText()))
                .count();
        assertEquals(2, alertSections);

        JsonNode firstButton = blocks.findValues("accessory").get(0);
        assertEquals("alert_delete", firstButton.get("action_id").asText());
        assertEquals("텔레칩스|310", firstButton.get("value").asText());
    }

    @Test
    void alertListAfterDelete_replaces_original_with_deleted_message_then_refreshed_alert_list() throws Exception {
        String json = builder.alertListAfterDelete(
                List.of(BusAlertResponse.from(BusAlert.create("U123", "벤처타운(북문)", "55", 3, LocalTime.of(8, 0), LocalTime.of(9, 30)))),
                "🗑️ 텔레칩스 310번 알림을 삭제했어요."
        );

        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode blocks = root.get("blocks");
        assertEquals("ephemeral", root.get("response_type").asText());
        assertTrue(root.get("replace_original").asBoolean());
        assertEquals("context", blocks.get(0).get("type").asText());
        assertEquals("🗑️ 텔레칩스 310번 알림을 삭제했어요.",
                blocks.get(0).get("elements").get(0).get("text").asText());
        assertEquals("divider", blocks.get(1).get("type").asText());
        assertEquals("header", blocks.get(2).get("type").asText());
        assertTrue(json.contains("🔔 등록된 버스 알림"));
        assertTrue(json.contains("벤처타운(북문)"));
    }

    @Test
    void alertListAfterDelete_when_no_alerts_left_shows_empty_notice() throws Exception {
        String json = builder.alertListAfterDelete(List.of(), "🗑️ 텔레칩스 310번 알림을 삭제했어요.");

        assertTrue(json.contains("텔레칩스 310번 알림을 삭제했어요."));
        assertTrue(json.contains("등록된 버스 알림이 없어요."));
    }

    @Test
    void boardedAcknowledgement_shows_all_alerts_are_suppressed_today() {
        String json = builder.boardedAcknowledgement("🚌 좋은 하루 보내세요! 오늘은 더 이상 모든 버스 알림이 울리지 않습니다.");

        assertTrue(json.contains("\"replace_original\":true"));
        assertTrue(json.contains("모든 버스 알림이 울리지 않습니다."));
    }

    @Test
    void ephemeralText_returns_message_visible_only_to_request_user() throws Exception {
        String json = builder.ephemeralText("✅ 버스 알림을 등록했어요.");

        JsonNode root = OBJECT_MAPPER.readTree(json);
        assertEquals("ephemeral", root.get("response_type").asText());
        assertEquals("✅ 버스 알림을 등록했어요.", root.get("text").asText());
    }

    @Test
    void stationArrival_renders_header_and_arrival_sections() throws Exception {
        String json = builder.stationArrival(StationArrivalView.success("텔레칩스", List.of(
                new BusArrivalResult("310", 4, 13),
                new BusArrivalResult("55", 7, null)
        )));

        JsonNode root = OBJECT_MAPPER.readTree(json);
        assertEquals("ephemeral", root.get("response_type").asText());
        assertTrue(json.contains("텔레칩스 정류장 도착 정보"));
        assertTrue(json.contains("310번"));
        assertTrue(json.contains("4분 후"));
        assertTrue(json.contains("55번"));
        assertTrue(json.contains("7분 후"));
    }

    @Test
    void stationArrival_when_empty_shows_no_bus_notice() {
        String json = builder.stationArrival(StationArrivalView.success("텔레칩스", List.of()));

        assertTrue(json.contains("등록된 버스 정보가 없어요."));
    }

    @Test
    void stationArrival_when_error_returns_error_card() {
        String json = builder.stationArrival(StationArrivalView.error("지원하지 않는 정류장이에요."));

        assertTrue(json.contains("지원하지 않는 정류장이에요."));
    }

    @Test
    void busArrival_renders_arrival_details() {
        String json = builder.busArrival(BusArrivalView.found(
                "텔레칩스", "310", new BusArrivalResult("310", 4, 13)
        ));

        assertTrue(json.contains("310번 버스 도착 정보"));
        assertTrue(json.contains("4분 후"));
        assertTrue(json.contains("13분 후"));
    }

    @Test
    void busArrival_when_not_found_shows_notice() {
        String json = builder.busArrival(BusArrivalView.notFound("텔레칩스", "999"));

        assertTrue(json.contains("현재 도착 예정 정보가 없어요."));
        assertTrue(json.contains("999번"));
    }

    @Test
    void alertList_includes_header_block() throws Exception {
        String json = builder.alertList(List.of(
                BusAlertResponse.from(BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30)))
        ));

        JsonNode blocks = OBJECT_MAPPER.readTree(json).get("blocks");
        assertEquals("header", blocks.get(0).get("type").asText());
    }
}
