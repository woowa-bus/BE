package com.woowa.bus.alert.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class BusAlert {

    private static final int MIN_NOTIFY_BEFORE_MINUTES = 1;
    private static final int MAX_NOTIFY_BEFORE_MINUTES = 30;

    private final Long id;
    private final String slackUserId;
    private final String stationName;
    private final String busNumber;
    private int notifyBeforeMinutes;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime lastNotifiedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BusAlert(
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
        validateNotifyBeforeMinutes(notifyBeforeMinutes);
        validateTimeRange(startTime, endTime);
        this.id = id;
        this.slackUserId = Objects.requireNonNull(slackUserId);
        this.stationName = Objects.requireNonNull(stationName);
        this.busNumber = Objects.requireNonNull(busNumber);
        this.notifyBeforeMinutes = notifyBeforeMinutes;
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.lastNotifiedAt = lastNotifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static BusAlert create(
            String slackUserId,
            String stationName,
            String busNumber,
            int notifyBeforeMinutes,
            LocalTime startTime,
            LocalTime endTime
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new BusAlert(
                null,
                slackUserId,
                stationName,
                busNumber,
                notifyBeforeMinutes,
                startTime,
                endTime,
                null,
                now,
                now
        );
    }

    public static BusAlert restore(
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
        return new BusAlert(
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

    public void updateNotificationRule(int notifyBeforeMinutes, LocalTime startTime, LocalTime endTime) {
        validateNotifyBeforeMinutes(notifyBeforeMinutes);
        validateTimeRange(startTime, endTime);
        this.notifyBeforeMinutes = notifyBeforeMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canSendNotification(LocalDateTime now, Integer predictTime, int cooldownMinutes) {
        return isWithinAlertTime(now.toLocalTime())
                && isArrivingSoon(predictTime)
                && isCooldownFinished(now, cooldownMinutes);
    }

    public void markNotified(LocalDateTime notifiedAt) {
        this.lastNotifiedAt = notifiedAt;
        this.updatedAt = notifiedAt;
    }

    public void markBoardedToday(LocalDateTime now) {
        this.lastNotifiedAt = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
        this.updatedAt = now;
    }

    public void resetNotification(LocalDateTime resetAt) {
        this.lastNotifiedAt = null;
        this.updatedAt = resetAt;
    }

    public boolean hasSlackUserId(String slackUserId) {
        return this.slackUserId.equals(slackUserId);
    }

    public boolean isSameTarget(String slackUserId, String stationName, String busNumber) {
        return this.slackUserId.equals(slackUserId)
                && this.stationName.equals(stationName)
                && this.busNumber.equals(busNumber);
    }

    public Long id() {
        return id;
    }

    public String slackUserId() {
        return slackUserId;
    }

    public String stationName() {
        return stationName;
    }

    public String busNumber() {
        return busNumber;
    }

    public int notifyBeforeMinutes() {
        return notifyBeforeMinutes;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public LocalDateTime lastNotifiedAt() {
        return lastNotifiedAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    private boolean isWithinAlertTime(LocalTime now) {
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    private boolean isArrivingSoon(Integer predictTime) {
        return predictTime != null && predictTime <= notifyBeforeMinutes;
    }

    private boolean isCooldownFinished(LocalDateTime now, int cooldownMinutes) {
        return lastNotifiedAt == null
                || Duration.between(lastNotifiedAt, now).toMinutes() >= cooldownMinutes;
    }

    private static void validateNotifyBeforeMinutes(int notifyBeforeMinutes) {
        if (notifyBeforeMinutes < MIN_NOTIFY_BEFORE_MINUTES || notifyBeforeMinutes > MAX_NOTIFY_BEFORE_MINUTES) {
            throw new BusAlertException("""
                    알림 기준 시간은 1~30분 사이로 입력해 주세요.

                    예시:
                    /알림 텔레칩스 310 5 17:45 23:30""");
        }
    }

    private static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusAlertException("""
                    종료 시간은 시작 시간보다 늦어야 해요.
                    MVP에서는 자정을 넘기는 알림 시간을 지원하지 않아요.

                    예시:
                    /알림 텔레칩스 310 5 17:45 23:30""");
        }
    }
}
