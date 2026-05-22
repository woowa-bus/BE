package com.woowa.bus.slack.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackModalBuilderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void alertCreateModal_contains_callback_id_and_inputs() throws Exception {
        SlackModalBuilder builder = new SlackModalBuilder(registry());

        String json = builder.alertCreateModal();
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals("modal", root.get("type").asText());
        assertEquals("alert_create", root.get("callback_id").asText());

        JsonNode blocks = root.get("blocks");
        assertEquals(5, blocks.size());
        assertTrue(json.contains("station_select"));
        assertTrue(json.contains("bus_input"));
        assertTrue(json.contains("notify_before_input"));
        assertTrue(json.contains("start_time_input"));
        assertTrue(json.contains("end_time_input"));
    }

    @Test
    void alertCreateModal_populates_station_options_from_registry() throws Exception {
        SlackModalBuilder builder = new SlackModalBuilder(registry());

        String json = builder.alertCreateModal();

        assertTrue(json.contains("텔레칩스"));
        assertTrue(json.contains("벤처타운(북문)"));
    }

    private BusRouteRegistry registry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                )),
                SupportedBusStation.of("벤처타운(북문)", "200000002", List.of(
                        SupportedBusRoute.of("310", "234000003", "14")
                ))
        ));
    }
}
