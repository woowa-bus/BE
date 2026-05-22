package com.woowa.bus.arrival.infrastructure;

import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
public class CachedBusArrivalClient implements BusArrivalClient {

    private final BusArrivalClient delegate;
    private final Clock clock;
    private final Duration ttl;
    private final Map<String, CachedArrivals> cache = new ConcurrentHashMap<>();

    public CachedBusArrivalClient(
            @Qualifier("gbisBusArrivalClient") BusArrivalClient delegate,
            Clock clock,
            @Value("${app.arrival.cache-ttl-seconds:30}") int ttlSeconds
    ) {
        this.delegate = delegate;
        this.clock = clock;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public List<BusArrivalResult> getArrivals(String stationId) {
        Instant now = clock.instant();
        CachedArrivals cached = cache.get(stationId);
        if (cached != null && cached.isFresh(now, ttl)) {
            log.debug("Using cached GBIS arrivals. stationId={}, ageSeconds={}", stationId, cached.ageSeconds(now));
            return cached.arrivals();
        }

        List<BusArrivalResult> arrivals = delegate.getArrivals(stationId);
        cache.put(stationId, new CachedArrivals(now, List.copyOf(arrivals)));
        log.debug("Cached GBIS arrivals. stationId={}, arrivalCount={}, ttlSeconds={}", stationId, arrivals.size(), ttl.toSeconds());
        return arrivals;
    }

    private record CachedArrivals(Instant fetchedAt, List<BusArrivalResult> arrivals) {

        boolean isFresh(Instant now, Duration ttl) {
            return Duration.between(fetchedAt, now).compareTo(ttl) < 0;
        }

        long ageSeconds(Instant now) {
            return Duration.between(fetchedAt, now).toSeconds();
        }
    }
}
