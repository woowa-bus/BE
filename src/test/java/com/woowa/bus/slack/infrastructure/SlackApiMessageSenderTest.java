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
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendDm_success() {
        SlackApiMessageSender sender = new SlackApiMessageSender(
                "xoxb-test",
                "http://localhost:%d/chat.postMessage".formatted(server.getAddress().getPort())
        );

        sender.sendDm("U123", "hello");

        assertTrue(authorization.contains("Bearer xoxb-test"));
        assertTrue(requestBody.contains("\"channel\":\"U123\""));
        assertTrue(requestBody.contains("\"text\":\"hello\""));
    }
}
