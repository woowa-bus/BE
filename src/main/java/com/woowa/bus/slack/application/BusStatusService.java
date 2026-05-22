package com.woowa.bus.slack.application;

import com.woowa.bus.arrival.application.BusArrivalMetrics;
import com.woowa.bus.arrival.application.BusArrivalMetricsSnapshot;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class BusStatusService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BusArrivalMetrics busArrivalMetrics;

    public BusStatusService(BusArrivalMetrics busArrivalMetrics) {
        this.busArrivalMetrics = busArrivalMetrics;
    }

    public String status() {
        BusArrivalMetricsSnapshot snapshot = busArrivalMetrics.snapshot();
        return """
                🩺 *시스템 상태*

                *GBIS API*
                %s""".formatted(gbisSection(snapshot));
    }

    private String gbisSection(BusArrivalMetricsSnapshot snapshot) {
        if (snapshot.totalCalls() == 0) {
            return "• 호출 없음";
        }
        return """
                • 호출: 총 %d회
                • 성공: %d회
                • 실패: %d회
                • 성공률: %.1f%%
                • 평균 응답 시간: %dms
                • 마지막 성공: %s
                • 마지막 실패: %s
                • 마지막 실패 원인: %s""".formatted(
                snapshot.totalCalls(),
                snapshot.successCount(),
                snapshot.failureCount(),
                snapshot.successRate() * 100,
                snapshot.averageLatencyMillis(),
                formatTimestamp(snapshot.lastSuccessAt()),
                formatTimestamp(snapshot.lastFailureAt()),
                snapshot.lastFailureMessage() == null ? "없음" : snapshot.lastFailureMessage()
        );
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "없음";
        }
        return timestamp.format(TIME_FORMATTER);
    }
}
