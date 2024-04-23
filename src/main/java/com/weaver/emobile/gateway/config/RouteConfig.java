package com.weaver.emobile.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.core.Ordered;

public class RouteConfig {

    public RouteLocator defaultRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("default", r -> r.order(Ordered.LOWEST_PRECEDENCE)
                        .path("/**")
                        .uri("http://192.168.1.241:8083"))
                .build();
    }

}
