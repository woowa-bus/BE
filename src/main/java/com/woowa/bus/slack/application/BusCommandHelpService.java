package com.woowa.bus.slack.application;

import com.woowa.bus.route.domain.BusRouteRegistry;
import org.springframework.stereotype.Service;

@Service
public class BusCommandHelpService {

    private final BusRouteRegistry busRouteRegistry;

    public BusCommandHelpService(BusRouteRegistry busRouteRegistry) {
        this.busRouteRegistry = busRouteRegistry;
    }

    public String help() {
        return """
                🚌 *버스 알리미 명령어 안내*

                *조회*
                • `/조회 텔레칩스`
                • `/조회 텔레칩스 310`

                *알림*
                • `/알림`
                • `/알림 텔레칩스 310 5 17:45 23:30`
                • `/알림 초기화`
                • `/알림목록`
                • `/알림삭제 텔레칩스 310`

                *설정*
                • 지원 정류장: %s
                • 알림 기준 시간: 1~30분
                • 시간 형식: HH:mm
                • 자정을 넘기는 알림 시간은 아직 지원하지 않아요.""".formatted(
                String.join(", ", busRouteRegistry.stationNames())
        );
    }
}
