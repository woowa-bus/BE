package com.woowa.bus.arrival.application;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class BusArrivalMetrics {

    private final Clock clock;

    private long totalCalls;
    private long successCount;
    private long failureCount;
    private long totalLatencyMillis;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastFailureAt;
    private String lastFailureMessage;

    public BusArrivalMetrics(Clock clock) {
        this.clock = clock;
    }

    public synchronized void recordSuccess(long latencyMillis) {
        totalCalls++;
        successCount++;
        totalLatencyMillis += latencyMillis;
        lastSuccessAt = LocalDateTime.now(clock);
    }

    public synchronized void recordFailure(String message) {
        totalCalls++;
        failureCount++;
        lastFailureAt = LocalDateTime.now(clock);
        lastFailureMessage = message;
    }

    public synchronized BusArrivalMetricsSnapshot snapshot() {
        long averageLatencyMillis = successCount == 0 ? 0 : totalLatencyMillis / successCount;
        return new BusArrivalMetricsSnapshot(
                totalCalls,
                successCount,
                failureCount,
                averageLatencyMillis,
                lastSuccessAt,
                lastFailureAt,
                lastFailureMessage
        );
    }
}
