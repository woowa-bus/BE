package com.woowa.bus.message;

import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class BusMessageFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private BusMessageFormatter() {
    }

    public static String stationArrival(String stationName, List<BusArrivalResult> arrivals) {
        if (arrivals == null || arrivals.isEmpty()) {
            return noRegisteredBus(stationName);
        }
        String body = arrivals.stream()
                .map(BusMessageFormatter::stationArrivalLine)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("현재 도착 예정 정보가 없어요.");
        return """
                🚌 %s 정류장 도착 정보

                %s""".formatted(stationName, body);
    }

    public static String busArrival(String stationName, String busNumber, BusArrivalResult arrival) {
        return """
                🚌 %s번 버스 도착 정보

                정류장: %s
                %s
                %s""".formatted(
                busNumber,
                stationName,
                arrivalFirstLine(arrival),
                arrivalSecondLine(arrival)
        );
    }

    public static String noRegisteredBus(String stationName) {
        return """
                등록된 버스 정보가 없어요.

                정류장: %s""".formatted(stationName);
    }

    public static String noArrival(String stationName, String busNumber) {
        return """
                현재 도착 예정 정보가 없어요.

                정류장: %s
                버스: %s번""".formatted(stationName, busNumber);
    }

    public static String busApiFailure() {
        return """
                버스 정보를 가져오지 못했어요.
                잠시 후 다시 시도해 주세요.""";
    }

    public static String alertCreated(String title, BusAlertCreateCommand command) {
        return """
                %s

                정류장: %s
                버스: %s번
                알림 기준: 도착 %d분 전
                알림 시간: %s~%s
                알림 방식: DM""".formatted(
                title,
                command.stationName(),
                command.busNumber(),
                command.notifyBeforeMinutes(),
                command.startTime().format(TIME_FORMATTER),
                command.endTime().format(TIME_FORMATTER)
        );
    }

    public static String alertDeleted(BusAlert alert) {
        return "🗑️ %s %s번 알림을 삭제했어요.".formatted(alert.stationName(), alert.busNumber());
    }

    public static String alertNotification(BusAlert alert, BusArrivalResult arrival, LocalDateTime now) {
        return """
                🔔 %s번 버스가 곧 도착해요!

                현재 시각: %s
                정류장: %s
                예상 도착: %s
                다음 버스: %s
                알림 기준: %d분 전
                알림 시간: %s~%s""".formatted(
                alert.busNumber(),
                now.format(TIME_FORMATTER),
                alert.stationName(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2()),
                alert.notifyBeforeMinutes(),
                alert.startTime().format(TIME_FORMATTER),
                alert.endTime().format(TIME_FORMATTER)
        );
    }

    public static String alertList(List<BusAlertResponse> alerts) {
        StringBuilder builder = new StringBuilder("🔔 등록된 버스 알림\n\n");
        for (int index = 0; index < alerts.size(); index++) {
            BusAlertResponse alert = alerts.get(index);
            builder.append("%d. %s %s번\n".formatted(
                    index + 1,
                    alert.stationName(),
                    alert.busNumber()
            ));
            builder.append("   • 알림 기준: %d분 전\n".formatted(alert.notifyBeforeMinutes()));
            builder.append("   • 알림 시간: %s~%s".formatted(
                    alert.startTime().format(TIME_FORMATTER),
                    alert.endTime().format(TIME_FORMATTER)
            ));
            if (index < alerts.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    public static String searchUsage() {
        return """
                /조회 [정류장]
                /조회 [정류장] [버스번호]

                예시:
                /조회 텔레칩스
                /조회 텔레칩스 310""";
    }

    public static String alertUsage() {
        return """
                /알림 [정류장] [버스번호] [몇 분 전] [시작시간] [종료시간]

                예시:
                /알림 텔레칩스 310 5 17:45 23:30""";
    }

    public static String alertDeleteUsage() {
        return """
                /알림삭제 [정류장] [버스번호]

                예시:
                /알림삭제 텔레칩스 310""";
    }

    private static String stationArrivalLine(BusArrivalResult arrival) {
        if (!arrival.hasArrival()) {
            return "• %s번: 현재 도착 예정 정보가 없어요.".formatted(arrival.busNumber());
        }
        if (arrival.predictTime2() == null) {
            return "• %s번: %s".formatted(arrival.busNumber(), arrivalText(arrival.predictTime1()));
        }
        return "• %s번: %s / 다음 %s".formatted(
                arrival.busNumber(),
                arrivalText(arrival.predictTime1()),
                arrivalText(arrival.predictTime2())
        );
    }

    private static String arrivalFirstLine(BusArrivalResult arrival) {
        if (arrival.predictTime1() == null) {
            return "첫 번째 버스: 정보 없음";
        }
        return "첫 번째 버스: %s".formatted(arrivalText(arrival.predictTime1()));
    }

    private static String arrivalSecondLine(BusArrivalResult arrival) {
        if (arrival.predictTime2() == null) {
            return "두 번째 버스: 정보 없음";
        }
        return "두 번째 버스: %s".formatted(arrivalText(arrival.predictTime2()));
    }

    private static String arrivalText(Integer predictTime) {
        if (predictTime == null) {
            return "정보 없음";
        }
        return "%d분 후".formatted(predictTime);
    }
}
