package com.woowa.bus.slack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.route.domain.BusRouteRegistry;
import org.springframework.stereotype.Component;

@Component
public class SlackModalBuilder {

    public static final String CALLBACK_ALERT_CREATE = "alert_create";

    public static final String BLOCK_STATION = "station";
    public static final String BLOCK_BUS = "bus";
    public static final String BLOCK_NOTIFY_BEFORE = "notify_before";
    public static final String BLOCK_START_TIME = "start_time";
    public static final String BLOCK_END_TIME = "end_time";

    public static final String ACTION_STATION = "station_select";
    public static final String ACTION_BUS = "bus_input";
    public static final String ACTION_NOTIFY_BEFORE = "notify_before_input";
    public static final String ACTION_START_TIME = "start_time_input";
    public static final String ACTION_END_TIME = "end_time_input";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BusRouteRegistry busRouteRegistry;

    public SlackModalBuilder(BusRouteRegistry busRouteRegistry) {
        this.busRouteRegistry = busRouteRegistry;
    }

    public String alertCreateModal() {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("type", "modal");
        root.put("callback_id", CALLBACK_ALERT_CREATE);
        root.set("title", plainText("버스 알림 등록"));
        root.set("submit", plainText("알림 등록"));
        root.set("close", plainText("취소"));

        ArrayNode blocks = root.putArray("blocks");
        blocks.add(stationInput());
        blocks.add(busInput());
        blocks.add(notifyBeforeInput());
        blocks.add(timeInput(BLOCK_START_TIME, ACTION_START_TIME, "시작 시간"));
        blocks.add(timeInput(BLOCK_END_TIME, ACTION_END_TIME, "종료 시간"));

        return root.toString();
    }

    private ObjectNode stationInput() {
        ObjectNode input = inputBlock(BLOCK_STATION, "정류장");
        ObjectNode element = input.putObject("element");
        element.put("type", "static_select");
        element.put("action_id", ACTION_STATION);
        element.set("placeholder", plainText("정류장을 선택해 주세요"));
        ArrayNode options = element.putArray("options");
        for (String name : busRouteRegistry.stationNames()) {
            ObjectNode option = options.addObject();
            option.set("text", plainText(name));
            option.put("value", name);
        }
        return input;
    }

    private ObjectNode busInput() {
        ObjectNode input = inputBlock(BLOCK_BUS, "버스 번호");
        ObjectNode element = input.putObject("element");
        element.put("type", "plain_text_input");
        element.put("action_id", ACTION_BUS);
        element.set("placeholder", plainText("예: 310"));
        return input;
    }

    private ObjectNode notifyBeforeInput() {
        ObjectNode input = inputBlock(BLOCK_NOTIFY_BEFORE, "알림 기준 시간");
        ObjectNode element = input.putObject("element");
        element.put("type", "number_input");
        element.put("action_id", ACTION_NOTIFY_BEFORE);
        element.put("is_decimal_allowed", false);
        element.put("min_value", "1");
        element.put("max_value", "30");
        input.set("hint", plainText("도착 몇 분 전에 알려드릴까요? 1~30 사이 숫자를 입력해 주세요."));
        return input;
    }

    private ObjectNode timeInput(String blockId, String actionId, String label) {
        ObjectNode input = inputBlock(blockId, label);
        ObjectNode element = input.putObject("element");
        element.put("type", "timepicker");
        element.put("action_id", actionId);
        return input;
    }

    private ObjectNode inputBlock(String blockId, String label) {
        ObjectNode input = OBJECT_MAPPER.createObjectNode();
        input.put("type", "input");
        input.put("block_id", blockId);
        input.set("label", plainText(label));
        return input;
    }

    private ObjectNode plainText(String text) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("type", "plain_text");
        node.put("text", text);
        node.put("emoji", true);
        return node;
    }
}
