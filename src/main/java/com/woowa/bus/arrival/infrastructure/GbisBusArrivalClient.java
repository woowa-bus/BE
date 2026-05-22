package com.woowa.bus.arrival.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GbisBusArrivalClient implements BusArrivalClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serviceKey;
    private final String requestUrl;
    private final RestClient restClient;

    public GbisBusArrivalClient(
            @Value("${gbis.service-key:}") String serviceKey,
            @Value("${gbis.arrival-url:https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalItemv2}")
            String requestUrl
    ) {
        this.serviceKey = serviceKey;
        this.requestUrl = requestUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public BusArrivalResult getArrival(String stationId, String routeId, String staOrder) {
        try {
            String response = restClient.get()
                    .uri(requestUri(stationId, routeId, staOrder))
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RuntimeException exception) {
            throw new BusArrivalException("버스 정보를 가져오지 못했어요.", exception);
        }
    }

    private URI requestUri(String stationId, String routeId, String staOrder) {
        String query = "serviceKey=%s&stationId=%s&routeId=%s&staOrder=%s&format=json".formatted(
                encode(serviceKey),
                encode(stationId),
                encode(routeId),
                encode(staOrder)
        );
        return URI.create(requestUrl + "?" + query);
    }

    private BusArrivalResult parse(String response) {
        try {
            JsonNode item = arrivalItem(OBJECT_MAPPER.readTree(response));
            return new BusArrivalResult(
                    text(item, "routeName"),
                    integer(item, "predictTime1"),
                    integer(item, "predictTime2")
            );
        } catch (Exception exception) {
            throw new BusArrivalException("버스 정보를 가져오지 못했어요.", exception);
        }
    }

    private JsonNode arrivalItem(JsonNode root) {
        JsonNode item = root.path("response").path("msgBody").path("busArrivalItem");
        if (item.isArray()) {
            return item.isEmpty() ? OBJECT_MAPPER.createObjectNode() : item.get(0);
        }
        return item;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText();
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asInt();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
