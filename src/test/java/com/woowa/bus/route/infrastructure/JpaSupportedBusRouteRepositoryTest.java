package com.woowa.bus.route.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaSupportedBusRouteRepository.class)
class JpaSupportedBusRouteRepositoryTest {

    @Autowired
    private SpringDataBusStationRepository springDataBusStationRepository;

    @Autowired
    private JpaSupportedBusRouteRepository repository;

    @Test
    void findAll_success() {
        BusStationJpaEntity station = BusStationJpaEntity.of(
                "텔레칩스",
                "204000158",
                "05341",
                "성남",
                "N",
                127.0873667,
                37.40645
        );
        station.addRoute("310", "234000001", "12");
        springDataBusStationRepository.save(station);

        List<SupportedBusStation> stations = repository.findAll();

        assertEquals(1, stations.size());
        assertEquals("텔레칩스", stations.getFirst().name());
        assertEquals("204000158", stations.getFirst().stationId());
        assertEquals(List.of("310"), stations.getFirst().busNumbers());
    }
}
