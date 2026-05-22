package com.woowa.bus.slack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlackBlockKitBuilder {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String alertList(List<BusAlertResponse> alerts) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "in_channel");

        ArrayNode blocks = root.putArray("blocks");
        blocks.add(header("🔔 등록된 버스 알림"));
        for (BusAlertResponse alert : alerts) {
            blocks.add(alertSection(alert));
        }

        return root.toString();
    }

    private ObjectNode header(String text) {
        ObjectNode header = OBJECT_MAPPER.createObjectNode();
        header.put("type", "header");
        ObjectNode textNode = header.putObject("text");
        textNode.put("type", "plain_text");
        textNode.put("text", text);
        textNode.put("emoji", true);
        return header;
    }

    private ObjectNode alertSection(BusAlertResponse alert) {
        ObjectNode section = OBJECT_MAPPER.createObjectNode();
        section.put("type", "section");

        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        text.put("text", "*%s* / %s번\n도착 %d분 전 / %s~%s".formatted(
                alert.stationName(),
                alert.busNumber(),
                alert.notifyBeforeMinutes(),
                alert.startTime().format(TIME_FORMATTER),
                alert.endTime().format(TIME_FORMATTER)
        ));

        ObjectNode accessory = section.putObject("accessory");
        accessory.put("type", "button");
        ObjectNode buttonText = accessory.putObject("text");
        buttonText.put("type", "plain_text");
        buttonText.put("text", "삭제");
        buttonText.put("emoji", true);
        accessory.put("action_id", "alert_delete");
        accessory.put("value", "%s|%s".formatted(alert.stationName(), alert.busNumber()));
        accessory.put("style", "danger");

        return section;
    }
}
