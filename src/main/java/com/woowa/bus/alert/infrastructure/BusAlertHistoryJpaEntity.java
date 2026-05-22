package com.woowa.bus.alert.infrastructure;

import com.woowa.bus.alert.domain.BusAlertHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bus_alert_histories",
        indexes = {
                @Index(name = "idx_bus_alert_histories_user", columnList = "slack_user_id"),
                @Index(name = "idx_bus_alert_histories_notified_at", columnList = "notified_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BusAlertHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slackUserId;

    @Column(nullable = false)
    private String stationName;

    @Column(nullable = false)
    private String busNumber;

    private Integer predictTime1;

    private Integer predictTime2;

    @Column(nullable = false)
    private LocalDateTime notifiedAt;

    private BusAlertHistoryJpaEntity(
            String slackUserId,
            String stationName,
            String busNumber,
            Integer predictTime1,
            Integer predictTime2,
            LocalDateTime notifiedAt
    ) {
        this.slackUserId = slackUserId;
        this.stationName = stationName;
        this.busNumber = busNumber;
        this.predictTime1 = predictTime1;
        this.predictTime2 = predictTime2;
        this.notifiedAt = notifiedAt;
    }

    static BusAlertHistoryJpaEntity from(BusAlertHistory history) {
        return new BusAlertHistoryJpaEntity(
                history.slackUserId(),
                history.stationName(),
                history.busNumber(),
                history.predictTime1(),
                history.predictTime2(),
                history.notifiedAt()
        );
    }

    BusAlertHistory toDomain() {
        return new BusAlertHistory(
                id,
                slackUserId,
                stationName,
                busNumber,
                predictTime1,
                predictTime2,
                notifiedAt
        );
    }
}
