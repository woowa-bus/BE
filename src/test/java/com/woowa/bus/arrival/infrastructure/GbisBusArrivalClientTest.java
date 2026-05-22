package com.woowa.bus.arrival.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GbisBusArrivalClientTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/getBusArrivalItemv2", exchange -> {
            String response = """
                    {
                      "response": {
                        "msgBody": {
                          "busArrivalItem": {
                            "routeName": "310",
                            "predictTime1": 4,
                            "predictTime2": 13
                          }
                        }
                      }
                    }""";
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
    void getArrival_success() {
        GbisBusArrivalClient client = new GbisBusArrivalClient(
                "test-key",
                "http://localhost:%d/getBusArrivalItemv2".formatted(server.getAddress().getPort())
        );

        BusArrivalResult result = client.getArrival("200000001", "234000001", "12");

        assertEquals("310", result.busNumber());
        assertEquals(4, result.predictTime1());
        assertEquals(13, result.predictTime2());
    }
}
