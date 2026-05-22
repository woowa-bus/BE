package com.woowa.bus.slack.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.woowa.bus.alert.application.BusAlertService;
import com.woowa.bus.alert.application.dto.BusAlertCreateCommand;
import com.woowa.bus.alert.application.dto.BusAlertDeleteCommand;
import com.woowa.bus.alert.application.dto.BusAlertResponse;
import com.woowa.bus.alert.domain.BusAlert;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import com.woowa.bus.search.application.BusArrivalSearchService;
import com.woowa.bus.search.application.BusArrivalView;
import com.woowa.bus.search.application.StationArrivalView;
import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRoute;
import com.woowa.bus.route.domain.SupportedBusStation;
import com.woowa.bus.slack.application.BusCommandHelpService;
import com.woowa.bus.slack.application.BusStatusService;
import com.woowa.bus.slack.application.SlackBlockKitBuilder;
import com.woowa.bus.slack.application.SlackModalBuilder;
import com.woowa.bus.slack.application.SlackViewsClient;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackCommandControllerTest {

    @Test
    void search_success_when_station_name_only() {
        SlackCommandController controller = controller();

        String response = controller.search("U123", "텔레칩스").getBody();

        assertTrue(response.contains("\"blocks\""));
        assertTrue(response.contains("텔레칩스 정류장 도착 정보"));
        assertTrue(response.contains("310번"));
    }

    @Test
    void search_success_when_station_name_and_bus_number() {
        SlackCommandController controller = controller();

        String response = controller.search("U123", "텔레칩스 310").getBody();

        assertTrue(response.contains("\"blocks\""));
        assertTrue(response.contains("310번 버스 도착 정보"));
        assertTrue(response.contains("4분 후"));
    }

    @Test
    void alert_success() {
        SlackCommandController controller = controller();

        String response = controller.alert("U123", "텔레칩스 310 5 17:45 23:30", "trigger123").getBody();

        assertTrue(response.contains("\"response_type\":\"ephemeral\""));
        assertTrue(response.contains("alert saved: 텔레칩스 310 5 17:45 23:30"));
    }

    @Test
    void alert_opens_modal_when_no_args_and_trigger_id_given() {
        FakeSlackViewsClient viewsClient = new FakeSlackViewsClient();
        SlackCommandController controller = controllerWith(viewsClient);

        String response = controller.alert("U123", "", "trigger999").getBody();

        assertEquals("", response);
        assertEquals("trigger999", viewsClient.lastTriggerId);
        assertTrue(viewsClient.lastViewJson.contains("alert_create"));
    }

    @Test
    void alert_reset_success() {
        SlackCommandController controller = controller();

        String response = controller.alert("U123", "초기화", "trigger123").getBody();

        assertTrue(response.contains("\"response_type\":\"ephemeral\""));
        assertTrue(response.contains("alert reset: U123"));
    }

    @Test
    void alert_fail_with_invalid_time_format() {
        SlackCommandController controller = controller();

        String response = controller.alert("U123", "텔레칩스 310 5 5:45 23:30", "trigger123").getBody();

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
                new FakeBusStatusService(),
                new SlackBlockKitBuilder(),
                new SlackModalBuilder(testRegistry()),
                new FakeSlackViewsClient()
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

    @Test
    void status_success() {
        SlackCommandController controller = controller();

        String response = controller.status("U123", "").getBody();

        assertEquals("status text", response);
    }

    private SlackCommandController controller() {
        return controllerWith(new FakeSlackViewsClient());
    }

    private SlackCommandController controllerWith(SlackViewsClient viewsClient) {
        return new SlackCommandController(
                new FakeBusArrivalSearchService(),
                new FakeBusAlertService(),
                new FakeBusCommandHelpService(),
                new FakeBusStatusService(),
                new SlackBlockKitBuilder(),
                new SlackModalBuilder(testRegistry()),
                viewsClient
        );
    }

    private BusRouteRegistry testRegistry() {
        return BusRouteRegistry.of(List.of(
                SupportedBusStation.of("텔레칩스", "200000001", List.of(
                        SupportedBusRoute.of("310", "234000001", "12")
                ))
        ));
    }

    private static class FakeSlackViewsClient implements SlackViewsClient {

        private String lastTriggerId;
        private String lastViewJson;

        @Override
        public void open(String triggerId, String viewJson) {
            this.lastTriggerId = triggerId;
            this.lastViewJson = viewJson;
        }
    }

    private static class FakeBusArrivalSearchService extends BusArrivalSearchService {

        FakeBusArrivalSearchService() {
            super(null, null);
        }

        @Override
        public StationArrivalView resolveStation(String stationName) {
            return StationArrivalView.success(stationName, List.of(
                    new BusArrivalResult("310", 4, 13)
            ));
        }

        @Override
        public BusArrivalView resolveBus(String stationName, String busNumber) {
            return BusArrivalView.found(stationName, busNumber, new BusArrivalResult(busNumber, 4, 13));
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

    private static class FakeBusStatusService extends BusStatusService {

        FakeBusStatusService() {
            super(null);
        }

        @Override
        public String status() {
            return "status text";
        }
    }

    private static class FakeBusAlertServiceWithAlert extends BusAlertService {

        FakeBusAlertServiceWithAlert() {
            super(null, null, null);
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
            super(null, null, null);
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

        @Override
        public String resetNotifications(String slackUserId) {
            return "alert reset: " + slackUserId;
        }
    }
}
