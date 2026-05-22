package com.woowa.bus.slack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.search.application.BusArrivalView;
import com.woowa.bus.search.application.StationArrivalView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlackBlockKitBuilder {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String alertList(List<BusAlertResponse> alerts) {
        return alertListResponse(alerts, false, null);
    }

    public String alertListAfterDelete(List<BusAlertResponse> alerts, String deletedMessage) {
        return alertListResponse(alerts, true, deletedMessage);
    }

    public String ephemeralText(String text) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");
        root.put("text", text);
        ArrayNode blocks = root.putArray("blocks");
        blocks.add(section(text));
        return root.toString();
    }

    private String alertListResponse(List<BusAlertResponse> alerts, boolean replaceOriginal, String deletedMessage) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");
        if (replaceOriginal) {
            root.put("replace_original", true);
        }

        ArrayNode blocks = root.putArray("blocks");
        if (deletedMessage != null && !deletedMessage.isBlank()) {
            blocks.add(context(deletedMessage));
            blocks.add(divider());
        }
        blocks.add(header("🔔 등록된 버스 알림"));
        if (alerts.isEmpty()) {
            blocks.add(context("ℹ️ 아직 등록된 버스 알림이 없어요."));
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

    private ObjectNode divider() {
        ObjectNode divider = OBJECT_MAPPER.createObjectNode();
        divider.put("type", "divider");
        return divider;
    }

    private ObjectNode section(String mrkdwn) {
        ObjectNode section = OBJECT_MAPPER.createObjectNode();
        section.put("type", "section");
        ObjectNode text = section.putObject("text");
        text.put("type", "mrkdwn");
        text.put("text", mrkdwn);
        return section;
    }

    public String alertNotificationBlocks(BusAlert alert, BusArrivalResult arrival, LocalDateTime now) {
        ArrayNode blocks = OBJECT_MAPPER.createArrayNode();
        blocks.add(header("🔔 %s번 버스가 곧 도착해요!".formatted(alert.busNumber())));

        blocks.add(section("""
                • 현재 시각: %s
                • 정류장: %s
                • 예상 도착: %s
                • 다음 버스: %s
                • 알림 기준: 도착 %d분 전
                • 알림 시간: %s~%s""".formatted(
                now.format(TIME_FORMATTER),
                alert.stationName(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2()),
                alert.notifyBeforeMinutes(),
                alert.startTime().format(TIME_FORMATTER),
                alert.endTime().format(TIME_FORMATTER)
        )));

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
        blocks.add(section(message));
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
            blocks.add(context("ℹ️ 등록된 버스 정보가 없어요."));
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
            blocks.add(context("ℹ️ 현재 도착 예정 정보가 없어요.\n• 정류장: %s\n• 버스: %s번"
                    .formatted(view.stationName(), view.busNumber())));
        } else {
            blocks.add(section("""
                    • 정류장: %s
                    • 첫 번째 버스: %s
                    • 두 번째 버스: %s""".formatted(
                    view.stationName(),
                    arrivalText(view.arrival().predictTime1()),
                    arrivalText(view.arrival().predictTime2())
            )));
        }

        return root.toString();
    }

    private String errorResponse(String message) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("response_type", "ephemeral");
        ArrayNode blocks = root.putArray("blocks");
        blocks.add(section(message));
        return root.toString();
    }

    private ObjectNode arrivalSection(BusArrivalResult arrival) {
        if (!arrival.hasArrival()) {
            return section("*%s번*\n• 도착 예정 정보 없음".formatted(arrival.busNumber()));
        } else if (arrival.predictTime2() == null) {
            return section("*%s번*\n• 첫 번째 버스: %s".formatted(arrival.busNumber(), arrivalText(arrival.predictTime1())));
        }
        return section("""
                *%s번*
                • 첫 번째 버스: %s
                • 다음 버스: %s""".formatted(
                arrival.busNumber(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2())
        ));
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
        text.put("text", """
                *%s %s번*
                • 알림 기준: 도착 %d분 전
                • 알림 시간: %s~%s""".formatted(
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
