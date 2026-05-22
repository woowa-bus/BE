package com.woowa.bus.arrival.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusArrivalMetricsTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    void snapshot_starts_empty() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:00:00Z"));

        BusArrivalMetricsSnapshot snapshot = metrics.snapshot();

        assertEquals(0, snapshot.totalCalls());
        assertEquals(0, snapshot.successCount());
        assertEquals(0, snapshot.failureCount());
        assertEquals(0, snapshot.averageLatencyMillis());
        assertNull(snapshot.lastSuccessAt());
        assertNull(snapshot.lastFailureAt());
        assertNull(snapshot.lastFailureMessage());
    }

    @Test
    void recordSuccess_accumulates_latency_and_timestamp() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:00:00Z"));

        metrics.recordSuccess(100);
        metrics.recordSuccess(300);

        BusArrivalMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(2, snapshot.totalCalls());
        assertEquals(2, snapshot.successCount());
        assertEquals(0, snapshot.failureCount());
        assertEquals(200, snapshot.averageLatencyMillis());
        assertEquals(LocalDateTime.of(2026, 5, 22, 18, 0), snapshot.lastSuccessAt());
    }

    @Test
    void recordFailure_tracks_message_and_timestamp() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:30:00Z"));

        metrics.recordFailure("read timeout");

        BusArrivalMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(1, snapshot.totalCalls());
        assertEquals(1, snapshot.failureCount());
        assertEquals("read timeout", snapshot.lastFailureMessage());
        assertEquals(LocalDateTime.of(2026, 5, 22, 18, 30), snapshot.lastFailureAt());
    }

    @Test
    void success_rate_uses_total_calls() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(fixedClock("2026-05-22T09:00:00Z"));

        metrics.recordSuccess(100);
        metrics.recordSuccess(100);
        metrics.recordSuccess(100);
        metrics.recordFailure("oops");

        assertEquals(0.75, metrics.snapshot().successRate());
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), KST);
    }
}
