package com.woowa.bus.arrival.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import com.woowa.bus.arrival.application.BusArrivalMetrics;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.time.Clock;
import java.time.ZoneId;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GbisBusArrivalClientTest {

    private static final String ENCODED_SERVICE_KEY = "test%2Fkey%3D%3D";

    private HttpServer server;
    private AtomicReference<String> requestQuery;

    @BeforeEach
    void setUp() throws IOException {
        requestQuery = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/getBusArrivalListv2", exchange -> {
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            String response = """
                    {
                      "response": {
                        "msgBody": {
                          "busArrivalList": [
                            {
                              "routeName": "310",
                              "predictTime1": 4,
                              "predictTime2": 13
                            }
                          ]
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
    void getArrivals_success() {
        BusArrivalMetrics metrics = new BusArrivalMetrics(Clock.system(ZoneId.of("Asia/Seoul")));
        GbisBusArrivalClient client = new GbisBusArrivalClient(
                ENCODED_SERVICE_KEY,
                "http://localhost:%d/getBusArrivalListv2".formatted(server.getAddress().getPort()),
                metrics
        );

        var results = client.getArrivals("200000001");

        assertEquals(1, results.size());
        assertEquals("310", results.getFirst().busNumber());
        assertEquals(4, results.getFirst().predictTime1());
        assertEquals(13, results.getFirst().predictTime2());
        assertEquals(
                "serviceKey=%s&stationId=200000001&format=json".formatted(ENCODED_SERVICE_KEY),
                requestQuery.get()
        );
        assertEquals(1, metrics.snapshot().successCount());
    }
}
