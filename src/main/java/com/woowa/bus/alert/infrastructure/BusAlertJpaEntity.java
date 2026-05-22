package com.woowa.bus.alert.infrastructure;

import com.woowa.bus.alert.domain.BusAlert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bus_alerts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_station_bus",
                        columnNames = {"slack_user_id", "station_name", "bus_number"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BusAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slackUserId;

    @Column(nullable = false)
    private String stationName;

    @Column(nullable = false)
    private String busNumber;

    @Column(nullable = false)
    private int notifyBeforeMinutes;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private LocalDateTime lastNotifiedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private BusAlertJpaEntity(
            Long id,
            String slackUserId,
            String stationName,
            String busNumber,
            int notifyBeforeMinutes,
            LocalTime startTime,
            LocalTime endTime,
            LocalDateTime lastNotifiedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.slackUserId = slackUserId;
        this.stationName = stationName;
        this.busNumber = busNumber;
        this.notifyBeforeMinutes = notifyBeforeMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastNotifiedAt = lastNotifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static BusAlertJpaEntity from(BusAlert alert) {
        return new BusAlertJpaEntity(
                alert.id(),
                alert.slackUserId(),
                alert.stationName(),
                alert.busNumber(),
                alert.notifyBeforeMinutes(),
                alert.startTime(),
                alert.endTime(),
                alert.lastNotifiedAt(),
                alert.createdAt(),
                alert.updatedAt()
        );
    }

    BusAlert toDomain() {
        return BusAlert.restore(
                id,
                slackUserId,
                stationName,
                busNumber,
                notifyBeforeMinutes,
                startTime,
                endTime,
                lastNotifiedAt,
                createdAt,
                updatedAt
        );
    }
}
