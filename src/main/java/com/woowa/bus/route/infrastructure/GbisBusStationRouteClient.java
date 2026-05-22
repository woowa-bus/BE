package com.woowa.bus.route.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.route.domain.SupportedBusRoute;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GbisBusStationRouteClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serviceKey;
    private final String requestUrl;
    private final RestClient restClient;

    public GbisBusStationRouteClient(
            @Value("${gbis.service-key:}") String serviceKey,
            @Value("${gbis.station-route-url:https://apis.data.go.kr/6410000/busstationservice/v2/getBusStationViaRouteListv2}")
            String requestUrl
    ) {
        this.serviceKey = serviceKey;
        this.requestUrl = requestUrl;
        this.restClient = RestClient.create();
    }

    public List<SupportedBusRoute> getRoutes(String stationId) {
        try {
            log.debug("Requesting GBIS station routes. stationId={}, url={}", stationId, requestUrl);
            String response = restClient.get()
                    .uri(requestUri(stationId))
                    .retrieve()
                    .body(String.class);
            List<SupportedBusRoute> routes = parse(response);
            log.info("GBIS station routes loaded. stationId={}, routeCount={}", stationId, routes.size());
            return routes;
        } catch (RuntimeException exception) {
            log.warn("GBIS station routes request failed. stationId={}", stationId, exception);
            return List.of();
        }
    }

    private URI requestUri(String stationId) {
        String query = "serviceKey=%s&stationId=%s&format=json".formatted(
                serviceKey,
                encode(stationId)
        );
        return URI.create(requestUrl + "?" + query);
    }

    private List<SupportedBusRoute> parse(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode items = root.path("response").path("msgBody").path("busRouteList");
            if (items.isMissingNode() || items.isNull()) {
                return List.of();
            }
            if (items.isArray()) {
                List<SupportedBusRoute> routes = new ArrayList<>();
                items.forEach(item -> addRoute(routes, item));
                return routes;
            }
            List<SupportedBusRoute> routes = new ArrayList<>();
            addRoute(routes, items);
            return routes;
        } catch (Exception exception) {
            log.warn("GBIS station routes parsing failed.", exception);
            return List.of();
        }
    }

    private void addRoute(List<SupportedBusRoute> routes, JsonNode item) {
        String routeId = text(item, "routeId");
        String routeName = text(item, "routeName");
        String staOrder = text(item, "staOrder");
        if (routeId.isBlank() || routeName.isBlank() || staOrder.isBlank()) {
            return;
        }
        routes.add(SupportedBusRoute.of(routeName, routeId, staOrder));
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
