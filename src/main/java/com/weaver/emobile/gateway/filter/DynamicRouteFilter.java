package com.weaver.emobile.gateway.filter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaver.emobile.gateway.util.Consts;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DynamicRouteFilter implements GlobalFilter, Ordered {
    private static final String GET_SETTING_URI = "/base/getsetting";

    private static Logger logger = LoggerFactory.getLogger(DynamicRouteFilter.class);

    private final ObjectMapper mapper;

    public DynamicRouteFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().toString();

        if (GET_SETTING_URI.equalsIgnoreCase(path)) {
            HttpHeaders headers = response.getHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> data = new HashMap<String, String>();
            data.put("keyEncryptType", "RSA");
            data.put("dataEncryptType", "AES");
            data.put("keyEncryptKey", Consts.PUBLIC_KEY);
            DataBuffer bodyDataBuffer = null;
            try {
                bodyDataBuffer = response.bufferFactory().wrap(this.mapper.writeValueAsBytes(data));
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage(), e);
            }
            ServerWebExchangeUtils.setAlreadyRouted(exchange);
            return response.writeWith(Flux.just(bodyDataBuffer));
        }
        String serverId = this.getServerId(request);
        String targetUrl = "";

        if ("1".equals(serverId)) {
            targetUrl = "http://127.0.0.1:8080";
        } else if ("2".equals(serverId)) {
            targetUrl = "http://192.168.1.241:8083";
        } else if ("3".equals(serverId)) {
            targetUrl = "https://www.weaver.com.cn";
        }

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
            return chain.filter(exchange);
        }
        response.setStatusCode(HttpStatus.NOT_FOUND);
        return response.setComplete();
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
