package com.weaver.emobile.gateway.filter;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

@Component
public class DynamicRouteFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String serverId = headers.getFirst("Server-Id");
        serverId = StringUtils.trim(serverId);
        String targetUrl = "";

        if ("1".equals(serverId)) {
            targetUrl = "http://127.0.0.1:8080";
        } else if ("2".equals(serverId)) {
            targetUrl = "https://www.weaver.com.cn";
        }

        if (StringUtils.isNotBlank(targetUrl)) {
            String rawPath = request.getURI().getRawPath();
            HttpMethod httpMethod = request.getMethod();
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            URI uri = UriComponentsBuilder.fromHttpUrl(targetUrl + rawPath).queryParams(queryParams).build().toUri();
            ServerHttpRequest serverHttpRequest = request.mutate().uri(uri).method(httpMethod).headers(httpHeaders -> httpHeaders = httpHeaders).build();
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            Route newRoute = Route.async()
                    .id(route.getId())
                    .asyncPredicate(route.getPredicate())
                    .filters(route.getFilters())
                    .order(route.getOrder())
                    .uri(uri)
                    .build();

            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, newRoute);
            return chain.filter(exchange.mutate().request(serverHttpRequest).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER;
    }

}
