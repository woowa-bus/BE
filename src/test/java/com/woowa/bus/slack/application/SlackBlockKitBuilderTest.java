package com.woowa.bus.slack.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
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

        assertEquals("in_channel", root.get("response_type").asText());
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
    void alertList_includes_header_block() throws Exception {
        String json = builder.alertList(List.of(
                BusAlertResponse.from(BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30)))
        ));

        JsonNode blocks = OBJECT_MAPPER.readTree(json).get("blocks");
        assertEquals("header", blocks.get(0).get("type").asText());
    }
}
