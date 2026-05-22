package com.woowa.bus.slack.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.slack.application.SlackMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SlackApiMessageSender implements SlackMessageSender {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    public void sendDm(String slackUserId, String text, String blocksJson) {
        try {
            log.debug("Sending Slack DM. userId={}, url={}, textLength={}, hasBlocks={}",
                    slackUserId, postMessageUrl, text == null ? 0 : text.length(), blocksJson != null);
            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.put("channel", slackUserId);
            body.put("text", text);
            if (blocksJson != null && !blocksJson.isBlank()) {
                JsonNode blocks = OBJECT_MAPPER.readTree(blocksJson);
                body.set("blocks", blocks);
            }
            restClient.post()
                    .uri(postMessageUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + botToken)
                    .body(body.toString())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Slack DM sent. userId={}", slackUserId);
        } catch (RuntimeException exception) {
            log.warn("Slack DM send failed. userId={}", slackUserId, exception);
            throw exception;
        } catch (Exception exception) {
            log.error("Slack DM blocks parse failed. userId={}", slackUserId, exception);
        }
    }

    @Override
    public void respond(String responseUrl, String bodyJson) {
        try {
            log.debug("Sending Slack interaction response. url={}, bodyLength={}",
                    responseUrl, bodyJson == null ? 0 : bodyJson.length());
            restClient.post()
                    .uri(responseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyJson)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Slack interaction response sent.");
        } catch (RuntimeException exception) {
            log.warn("Slack interaction response send failed.", exception);
            throw exception;
        }
    }
}
