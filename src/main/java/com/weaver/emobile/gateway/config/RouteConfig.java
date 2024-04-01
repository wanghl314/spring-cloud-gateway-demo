package com.weaver.emobile.gateway.config;

import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator defaultRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("default", r -> r.order(Ordered.LOWEST_PRECEDENCE)
                        .path("/**")
                        .uri("http://192.168.1.241:8083"))
                .build();
    }

}
