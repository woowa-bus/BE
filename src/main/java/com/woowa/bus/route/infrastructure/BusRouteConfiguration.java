package com.woowa.bus.route.infrastructure;

import com.woowa.bus.route.domain.BusRouteRegistry;
import com.woowa.bus.route.domain.SupportedBusRouteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusRouteConfiguration {

    @Bean
    public BusRouteRegistry busRouteRegistry(SupportedBusRouteRepository repository) {
        return new BusRouteRegistry(repository);
    }
}
