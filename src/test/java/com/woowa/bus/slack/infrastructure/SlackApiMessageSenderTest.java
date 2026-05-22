package com.woowa.bus.slack.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackApiMessageSenderTest {

    private HttpServer server;
    private String requestBody = "";
    private String responseRequestBody = "";
    private String authorization = "";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat.postMessage", exchange -> {
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String response = "{\"ok\":true}";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response.getBytes());
            }
        });
        server.createContext("/response", exchange -> {
            responseRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String response = "ok";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response.getBytes());
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendDm_success() {
        SlackApiMessageSender sender = new SlackApiMessageSender(
                "test-slack-token",
                "http://localhost:%d/chat.postMessage".formatted(server.getAddress().getPort())
        );

        sender.sendDm("U123", "hello", null);

        assertTrue(authorization.contains("Bearer test-slack-token"));
        assertTrue(requestBody.contains("\"channel\":\"U123\""));
        assertTrue(requestBody.contains("\"text\":\"hello\""));
    }

    @Test
    void sendDm_includes_blocks_when_provided() {
        SlackApiMessageSender sender = new SlackApiMessageSender(
                "test-slack-token",
                "http://localhost:%d/chat.postMessage".formatted(server.getAddress().getPort())
        );

        sender.sendDm("U123", "fallback", "[{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"hi\"}}]");

        assertTrue(requestBody.contains("\"blocks\":["));
        assertTrue(requestBody.contains("\"text\":\"hi\""));
    }

    @Test
    void respond_posts_body_to_response_url() {
        SlackApiMessageSender sender = new SlackApiMessageSender(
                "test-slack-token",
                "http://localhost:%d/chat.postMessage".formatted(server.getAddress().getPort())
        );

        sender.respond(
                "http://localhost:%d/response".formatted(server.getAddress().getPort()),
                "{\"replace_original\":true,\"text\":\"삭제했어요.\"}"
        );

        assertTrue(responseRequestBody.contains("\"replace_original\":true"));
        assertTrue(responseRequestBody.contains("삭제했어요."));
    }
}
