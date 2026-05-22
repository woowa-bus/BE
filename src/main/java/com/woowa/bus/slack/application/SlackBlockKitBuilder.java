package com.woowa.bus.slack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.search.application.BusArrivalView;
import com.woowa.bus.search.application.StationArrivalView;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlackBlockKitBuilder {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String alertList(List<BusAlertResponse> alerts) {
        return alertListResponse(alerts, false, false);
    }

    public String alertListAfterDelete(List<BusAlertResponse> alerts) {
        return alertListResponse(alerts, true, true);
    }

    private String alertListResponse(List<BusAlertResponse> alerts, boolean replaceOriginal, boolean deleted) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");
        if (replaceOriginal) {
            root.put("replace_original", true);
        }

        ArrayNode blocks = root.putArray("blocks");
        blocks.add(header("🔔 등록된 버스 알림"));
        if (deleted) {
            blocks.add(context("삭제되었습니다."));
        }
        if (alerts.isEmpty()) {
            blocks.add(context("등록된 버스 알림이 없어요."));
        } else {
            for (BusAlertResponse alert : alerts) {
                blocks.add(alertSection(alert));
            }
        }

        return root.toString();
    }

    private ObjectNode context(String text) {
        ObjectNode context = OBJECT_MAPPER.createObjectNode();
        context.put("type", "context");
        ArrayNode elements = context.putArray("elements");
        ObjectNode element = elements.addObject();
        element.put("type", "mrkdwn");
        element.put("text", text);
        return context;
    }

    public String alertNotificationBlocks(BusAlert alert, BusArrivalResult arrival) {
        ArrayNode blocks = OBJECT_MAPPER.createArrayNode();
        blocks.add(header("🔔 %s번 버스가 곧 도착해요!".formatted(alert.busNumber())));

        ObjectNode section = blocks.addObject();
        section.put("type", "section");
        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        text.put("text", "*정류장:* %s\n*예상 도착:* %s\n*다음 버스:* %s\n*알림 기준:* %d분 전\n*알림 시간:* %s~%s".formatted(
                alert.stationName(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2()),
                alert.notifyBeforeMinutes(),
                alert.startTime().format(TIME_FORMATTER),
                alert.endTime().format(TIME_FORMATTER)
        ));

        ObjectNode actions = blocks.addObject();
        actions.put("type", "actions");
        ArrayNode elements = actions.putArray("elements");
        ObjectNode button = elements.addObject();
        button.put("type", "button");
        ObjectNode buttonText = button.putObject("text");
        buttonText.put("type", "plain_text");
        buttonText.put("text", "🚌 탑승 완료");
        buttonText.put("emoji", true);
        button.put("action_id", "alert_boarded");
        button.put("value", "%s|%s".formatted(alert.stationName(), alert.busNumber()));
        button.put("style", "primary");

        return blocks.toString();
    }

    public String boardedAcknowledgement(String message) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("replace_original", true);
        ArrayNode blocks = root.putArray("blocks");
        ObjectNode section = blocks.addObject();
        section.put("type", "section");
        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        text.put("text", message);
        return root.toString();
    }

    public String stationArrival(StationArrivalView view) {
        if (view.isError()) {
            return errorResponse(view.errorMessage());
        }
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");

        ArrayNode blocks = root.putArray("blocks");
        blocks.add(header("🚌 %s 정류장 도착 정보".formatted(view.stationName())));
        if (view.arrivals().isEmpty()) {
            blocks.add(context("등록된 버스 정보가 없어요."));
        } else {
            for (BusArrivalResult arrival : view.arrivals()) {
                blocks.add(arrivalSection(arrival));
            }
        }

        return root.toString();
    }

    public String busArrival(BusArrivalView view) {
        if (view.isError()) {
            return errorResponse(view.errorMessage());
        }
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");

        ArrayNode blocks = root.putArray("blocks");
        blocks.add(header("🚌 %s번 버스 도착 정보".formatted(view.busNumber())));

        if (!view.hasArrival()) {
            blocks.add(context("현재 도착 예정 정보가 없어요.\n정류장: %s / 버스: %s번"
                    .formatted(view.stationName(), view.busNumber())));
        } else {
            ObjectNode section = blocks.addObject();
            section.put("type", "section");
            ObjectNode text = section.putObject("text");
            text.put("type", "mrkdwn");
            text.put("text", "*정류장:* %s\n*첫 번째 버스:* %s\n*두 번째 버스:* %s".formatted(
                    view.stationName(),
                    arrivalText(view.arrival().predictTime1()),
                    arrivalText(view.arrival().predictTime2())
            ));
        }

        return root.toString();
    }

    private String errorResponse(String message) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");
        ArrayNode blocks = root.putArray("blocks");
        ObjectNode section = blocks.addObject();
        section.put("type", "section");
        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        text.put("text", message);
        return root.toString();
    }

    private ObjectNode arrivalSection(BusArrivalResult arrival) {
        ObjectNode section = OBJECT_MAPPER.createObjectNode();
        section.put("type", "section");
        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        if (!arrival.hasArrival()) {
            text.put("text", "*%s번*\n현재 도착 예정 정보가 없어요.".formatted(arrival.busNumber()));
        } else if (arrival.predictTime2() == null) {
            text.put("text", "*%s번*\n%s".formatted(arrival.busNumber(), arrivalText(arrival.predictTime1())));
        } else {
            text.put("text", "*%s번*\n%s / 다음 %s".formatted(
                    arrival.busNumber(),
                    arrivalText(arrival.predictTime1()),
                    arrivalText(arrival.predictTime2())
            ));
        }
        return section;
    }

    private String arrivalText(Integer predictTime) {
        if (predictTime == null) {
            return "정보 없음";
        }
        return "%d분 후".formatted(predictTime);
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
