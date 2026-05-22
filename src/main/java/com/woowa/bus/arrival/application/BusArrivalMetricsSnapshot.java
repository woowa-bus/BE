package com.woowa.bus.arrival.application;

import java.time.LocalDateTime;

public record BusArrivalMetricsSnapshot(
        long totalCalls,
        long successCount,
        long failureCount,
        long averageLatencyMillis,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt,
        String lastFailureMessage
) {

    public double successRate() {
        if (totalCalls == 0) {
            return 0.0;
        }
        return (double) successCount / totalCalls;
    }
}
