package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.search.application.BusArrivalSearchService;
import com.woowa.bus.slack.application.BusCommandHelpService;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackCommandControllerTest {

    @Test
    void search_success_when_station_name_only() {
        SlackCommandController controller = controller();

        String response = controller.search("U123", "텔레칩스").getBody();

        assertEquals("station search: 텔레칩스", response);
    }

    @Test
    void search_success_when_station_name_and_bus_number() {
        SlackCommandController controller = controller();

        String response = controller.search("U123", "텔레칩스 310").getBody();

        assertEquals("bus search: 텔레칩스 310", response);
    }

    @Test
    void alert_success() {
        SlackCommandController controller = controller();

        String response = controller.alert("U123", "텔레칩스 310 5 17:45 23:30").getBody();

        assertEquals("alert saved: 텔레칩스 310 5 17:45 23:30", response);
    }

    @Test
    void alert_fail_with_invalid_time_format() {
        SlackCommandController controller = controller();

        String response = controller.alert("U123", "텔레칩스 310 5 5:45 23:30").getBody();

        assertEquals("""
                시간은 HH:mm 형식으로 입력해 주세요.

                예시:
                /알림 텔레칩스 310 5 17:45 23:30""", response);
    }

    @Test
    void alertList_success_when_empty() {
        SlackCommandController controller = controller();

        String response = controller.alertList("U123", "").getBody();

        assertEquals("등록된 버스 알림이 없어요.", response);
    }

    @Test
    void alertList_returns_block_kit_when_alerts_exist() {
        SlackCommandController controller = new SlackCommandController(
                new FakeBusArrivalSearchService(),
                new FakeBusAlertServiceWithAlert(),
                new FakeBusCommandHelpService(),
                new SlackBlockKitBuilder()
        );

        String response = controller.alertList("U123", "").getBody();

        assertTrue(response.contains("\"blocks\""));
        assertTrue(response.contains("alert_delete"));
        assertTrue(response.contains("텔레칩스|310"));
    }

    @Test
    void alertDelete_success() {
        SlackCommandController controller = controller();

        String response = controller.alertDelete("U123", "텔레칩스 310").getBody();

        assertEquals("alert deleted: 텔레칩스 310", response);
    }

    @Test
    void help_success() {
        SlackCommandController controller = controller();

        String response = controller.help("U123", "").getBody();

        assertEquals("help text", response);
    }

    private SlackCommandController controller() {
        return new SlackCommandController(
                new FakeBusArrivalSearchService(),
                new FakeBusAlertService(),
                new FakeBusCommandHelpService(),
                new SlackBlockKitBuilder()
        );
    }

    private static class FakeBusArrivalSearchService extends BusArrivalSearchService {

        FakeBusArrivalSearchService() {
            super(null, null);
        }

        @Override
        public String searchStation(String stationName) {
            return "station search: " + stationName;
        }

        @Override
        public String searchBus(String stationName, String busNumber) {
            return "bus search: " + stationName + " " + busNumber;
        }
    }

    private static class FakeBusCommandHelpService extends BusCommandHelpService {

        FakeBusCommandHelpService() {
            super(null);
        }

        @Override
        public String help() {
            return "help text";
        }
    }

    private static class FakeBusAlertServiceWithAlert extends BusAlertService {

        FakeBusAlertServiceWithAlert() {
            super(null, null);
        }

        @Override
        public List<BusAlertResponse> findAllBySlackUserId(String slackUserId) {
            List<BusAlertResponse> responses = new ArrayList<>();
            responses.add(BusAlertResponse.from(
                    BusAlert.create("U123", "텔레칩스", "310", 5, LocalTime.of(17, 45), LocalTime.of(23, 30))
            ));
            return responses;
        }
    }

    private static class FakeBusAlertService extends BusAlertService {

        FakeBusAlertService() {
            super(null, null);
        }

        @Override
        public String save(BusAlertCreateCommand command) {
            return "alert saved: %s %s %d %s %s".formatted(
                    command.stationName(),
                    command.busNumber(),
                    command.notifyBeforeMinutes(),
                    command.startTime(),
                    command.endTime()
            );
        }

        @Override
        public List<BusAlertResponse> findAllBySlackUserId(String slackUserId) {
            return List.of();
        }

        @Override
        public String delete(BusAlertDeleteCommand command) {
            return "alert deleted: " + command.stationName() + " " + command.busNumber();
        }
    }
}
