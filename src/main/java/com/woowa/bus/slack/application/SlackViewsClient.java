package com.woowa.bus.slack.application;

public interface SlackViewsClient {

    void open(String triggerId, String viewJson);
}
