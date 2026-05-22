package com.woowa.bus.slack.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.arrival.application.BusArrivalMetrics;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusStatusServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    void status_renders_no_call_state() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:00:00Z"));
        BusStatusService service = new BusStatusService(metrics);

        String message = service.status();

        assertTrue(message.contains("🩺 *시스템 상태*"));
        assertTrue(message.contains("GBIS API"));
        assertTrue(message.contains("호출 없음"));
    }

    @Test
    void status_renders_counts_and_success_rate() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:00:00Z"));
        metrics.recordSuccess(200);
        metrics.recordSuccess(400);
        metrics.recordSuccess(300);
        metrics.recordFailure("timeout");
        BusStatusService service = new BusStatusService(metrics);

        String message = service.status();

        assertTrue(message.contains("총 4회"));
        assertTrue(message.contains("성공: 3회"));
        assertTrue(message.contains("실패: 1회"));
        assertTrue(message.contains("75.0%"));
        assertTrue(message.contains("300ms"));
        assertTrue(message.contains("timeout"));
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), KST);
    }
}
