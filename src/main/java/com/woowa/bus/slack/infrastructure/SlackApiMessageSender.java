package com.woowa.bus.slack.infrastructure;

import com.woowa.bus.slack.application.SlackMessageSender;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackApiMessageSender implements SlackMessageSender {

    private final String botToken;
    private final String postMessageUrl;
    private final RestClient restClient;

    public SlackApiMessageSender(
            @Value("${slack.bot-token:}") String botToken,
            @Value("${slack.post-message-url:https://slack.com/api/chat.postMessage}") String postMessageUrl
    ) {
        this.botToken = botToken;
        this.postMessageUrl = postMessageUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public void sendDm(String slackUserId, String message) {
        restClient.post()
                .uri(postMessageUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + botToken)
                .body(Map.of(
                        "channel", slackUserId,
                        "text", message
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
