package com.woowa.bus.slack.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woowa.bus.slack.application.SlackViewsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SlackApiViewsClient implements SlackViewsClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String botToken;
    private final String viewsOpenUrl;
    private final RestClient restClient;

    public SlackApiViewsClient(
            @Value("${slack.bot-token:}") String botToken,
            @Value("${slack.views-open-url:https://slack.com/api/views.open}") String viewsOpenUrl
    ) {
        this.botToken = botToken;
        this.viewsOpenUrl = viewsOpenUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public void open(String triggerId, String viewJson) {
        try {
            JsonNode view = OBJECT_MAPPER.readTree(viewJson);
            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.put("trigger_id", triggerId);
            body.set("view", view);

            log.debug("Opening Slack view. triggerId={}, url={}", triggerId, viewsOpenUrl);
            restClient.post()
                    .uri(viewsOpenUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + botToken)
                    .body(body.toString())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Slack view opened. triggerId={}", triggerId);
        } catch (RuntimeException exception) {
            log.warn("Slack view open failed. triggerId={}", triggerId, exception);
            throw exception;
        } catch (Exception exception) {
            log.error("Slack view payload parse failed.", exception);
        }
    }
}
