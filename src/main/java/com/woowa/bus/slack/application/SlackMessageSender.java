package com.woowa.bus.slack.application;

public interface SlackMessageSender {

    void sendDm(String slackUserId, String message);
}
