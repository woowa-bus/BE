package com.woowa.bus.route.domain;

import java.util.List;
import java.util.Map;

final class BusStationNameResolver {

    private static final Map<String, String> ALIASES = Map.of(
            normalize("벤처타워(북문)"), "벤처타운(북문)"
    );

    private BusStationNameResolver() {
    }

    static String resolve(String stationName, List<SupportedBusStation> stations) {
        String normalizedInput = normalize(stationName);
        List<String> stationNames = stations.stream()
                .map(SupportedBusStation::name)
                .toList();

        String exactMatch = stationNames.stream()
                .filter(name -> name.equals(stationName))
                .findFirst()
                .orElse(null);
        if (exactMatch != null) {
            return exactMatch;
        }

        String normalizedMatch = stationNames.stream()
                .filter(name -> normalize(name).equals(normalizedInput))
                .findFirst()
                .orElse(null);
        if (normalizedMatch != null) {
            return normalizedMatch;
        }

        String aliasMatch = ALIASES.get(normalizedInput);
        if (aliasMatch != null) {
            return stationNames.stream()
                    .filter(name -> normalize(name).equals(normalize(aliasMatch)))
                    .findFirst()
                    .orElse(aliasMatch);
        }

        return stationName;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s()\\-_,.·]", "");
    }
}
