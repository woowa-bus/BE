package com.woowa.bus.arrival.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachedBusArrivalClientTest {

    @Test
    void getArrivals_uses_cache_within_ttl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-22T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        CountingBusArrivalClient delegate = new CountingBusArrivalClient();
        CachedBusArrivalClient client = new CachedBusArrivalClient(delegate, clock, 30);

        List<BusArrivalResult> first = client.getArrivals("200000001");
        List<BusArrivalResult> second = client.getArrivals("200000001");

        assertEquals(1, delegate.calls.get());
        assertEquals(first, second);
    }

    @Test
    void getArrivals_refreshes_after_ttl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-22T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        CountingBusArrivalClient delegate = new CountingBusArrivalClient();
        CachedBusArrivalClient client = new CachedBusArrivalClient(delegate, clock, 30);

        client.getArrivals("200000001");
        clock.plus(Duration.ofSeconds(31));
        client.getArrivals("200000001");

        assertEquals(2, delegate.calls.get());
    }

    private static class CountingBusArrivalClient implements BusArrivalClient {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public List<BusArrivalResult> getArrivals(String stationId) {
            calls.incrementAndGet();
            return List.of(new BusArrivalResult("310", 4, 13));
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
