package com.woowa.bus.route.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.woowa.bus.route.domain.SupportedBusRoute;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GbisBusStationRouteClientTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/getBusStationViaRouteListv2", exchange -> {
            String response = """
                    {
                      "response": {
                        "msgBody": {
                          "busRouteList": [
                            {
                              "routeId": "234000001",
                              "routeName": "310",
                              "staOrder": "12"
                            },
                            {
                              "routeId": "234000002",
                              "routeName": "55",
                              "staOrder": "13"
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
    void getRoutes_success() {
        GbisBusStationRouteClient client = new GbisBusStationRouteClient(
                "test-key",
                "http://localhost:%d/getBusStationViaRouteListv2".formatted(server.getAddress().getPort())
        );

        List<SupportedBusRoute> routes = client.getRoutes("204000158");

        assertEquals(2, routes.size());
        assertEquals("310", routes.getFirst().busNumber());
        assertEquals("234000001", routes.getFirst().routeId());
        assertEquals("12", routes.getFirst().staOrder());
    }
}
