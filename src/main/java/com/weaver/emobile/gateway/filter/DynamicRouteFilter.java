package com.weaver.emobile.gateway.filter;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.weaver.emobile.gateway.util.RouteUtils;

import reactor.core.publisher.Mono;

@Component
public class DynamicRouteFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String serverId = this.getServerId(request);
        String targetUrl = RouteUtils.getRoutes().get(serverId);

        if (StringUtils.isNotBlank(targetUrl)) {
            URI uri = UriComponentsBuilder.fromHttpUrl(targetUrl).build().toUri();
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            Route newRoute = Route.async()
                    .id(route.getId())
                    .uri(uri)
                    .order(route.getOrder())
                    .asyncPredicate(route.getPredicate())
                    .filters(route.getFilters())
                    .metadata(route.getMetadata())
                    .build();

            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, newRoute);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 30;
    }

    private String getServerId(ServerHttpRequest request) {
        String key = "Server-Id";
        String value = null;

        HttpCookie cookie = request.getCookies().getFirst(key);

        if (cookie != null) {
            value = cookie.getValue();
        }

        if (StringUtils.isBlank(value)) {
            value = request.getHeaders().getFirst(key);
        }
        return StringUtils.trim(value);
    }

}
