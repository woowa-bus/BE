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
                🚌 버스 알리미 명령어 안내

                /조회 [정류장]
                  예: /조회 텔레칩스

                /조회 [정류장] [버스번호]
                  예: /조회 텔레칩스 310

                /알림 [정류장] [버스번호] [몇 분 전] [시작시간] [종료시간]
                  예: /알림 텔레칩스 310 5 17:45 23:30

                /알림목록

                /알림삭제 [정류장] [버스번호]
                  예: /알림삭제 텔레칩스 310

                지원 정류장: %s
                알림 기준 시간: 1~30분
                시간 형식: HH:mm (자정 넘김 미지원)""".formatted(
                String.join(", ", busRouteRegistry.stationNames())
        );
    }
}
