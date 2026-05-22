package com.woowa.bus.slack.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusCommandHelpServiceTest {

    @Test
    void help_contains_all_commands() {
        BusCommandHelpService service = new BusCommandHelpService(registry());

        String message = service.help();

        assertTrue(message.contains("/조회"));
        assertTrue(message.contains("/알림"));
        assertTrue(message.contains("/알림 초기화"));
        assertTrue(message.contains("/알림목록"));
        assertTrue(message.contains("/알림삭제"));
    }

    @Test
    void help_contains_supported_stations_from_registry() {
        BusCommandHelpService service = new BusCommandHelpService(registry());

        String message = service.help();

        assertTrue(message.contains("텔레칩스"));
        assertTrue(message.contains("벤처타운(북문)"));
    }

    @Test
    void help_contains_usage_constraints() {
        BusCommandHelpService service = new BusCommandHelpService(registry());

        String message = service.help();

        assertTrue(message.contains("1~30분"));
        assertTrue(message.contains("HH:mm"));
    }

    private BusRouteRegistry registry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                )),
                SupportedBusStation.of("벤처타운(북문)", "200000002", List.of(
                        SupportedBusRoute.of("310", "234000003", "14")
                ))
        ));
    }
}
