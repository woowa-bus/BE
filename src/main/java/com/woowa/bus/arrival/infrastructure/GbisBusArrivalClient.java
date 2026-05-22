package com.woowa.bus.arrival.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowa.bus.arrival.domain.BusArrivalClient;
import com.woowa.bus.arrival.domain.BusArrivalException;
import com.woowa.bus.arrival.domain.BusArrivalResult;
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
public class GbisBusArrivalClient implements BusArrivalClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serviceKey;
    private final String requestUrl;
    private final RestClient restClient;

    public GbisBusArrivalClient(
            @Value("${gbis.service-key:}") String serviceKey,
            @Value("${gbis.endpoint-url:https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalListv2}")
            String requestUrl
    ) {
        this.serviceKey = serviceKey;
        this.requestUrl = requestUrl.endsWith("getBusArrivalListv2")
                ? requestUrl
                : requestUrl.endsWith("/")
                ? requestUrl + "getBusArrivalListv2"
                : requestUrl + "/getBusArrivalListv2";
        this.restClient = RestClient.create();
    }

    @Override
    public List<BusArrivalResult> getArrivals(String stationId) {
        try {
            log.debug("Requesting GBIS arrivals. stationId={}, url={}", stationId, requestUrl);
            String response = restClient.get()
                    .uri(requestUri(stationId))
                    .retrieve()
                    .body(String.class);
            log.debug("GBIS arrival response received. stationId={}, responseLength={}", stationId, response == null ? 0 : response.length());
            return parse(response);
        } catch (RuntimeException exception) {
            log.warn("GBIS arrival request failed. stationId={}", stationId, exception);
            throw new BusArrivalException("버스 정보를 가져오지 못했어요.", exception);
        }
    }

    private URI requestUri(String stationId) {
        String query = "serviceKey=%s&stationId=%s&format=json".formatted(
                serviceKey,
                encode(stationId)
        );
        return URI.create(requestUrl + "?" + query);
    }

    private List<BusArrivalResult> parse(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode items = root.path("response").path("msgBody").path("busArrivalList");
            if (items.isMissingNode() || items.isNull()) {
                log.debug("GBIS arrival list missing.");
                return List.of();
            }
            List<BusArrivalResult> arrivals = new ArrayList<>();
            if (items.isArray()) {
                items.forEach(item -> addArrival(arrivals, item));
            } else {
                addArrival(arrivals, items);
            }
            log.debug("GBIS arrival parsed successfully. arrivalCount={}", arrivals.size());
            return arrivals;
        } catch (Exception exception) {
            log.warn("GBIS arrival parsing failed.", exception);
            return List.of();
        }
    }

    private void addArrival(List<BusArrivalResult> arrivals, JsonNode item) {
        String routeName = text(item, "routeName");
        Integer predictTime1 = integer(item, "predictTime1");
        Integer predictTime2 = integer(item, "predictTime2");
        if (routeName.isBlank() && predictTime1 == null && predictTime2 == null) {
            return;
        }
        arrivals.add(new BusArrivalResult(routeName, predictTime1, predictTime2));
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
