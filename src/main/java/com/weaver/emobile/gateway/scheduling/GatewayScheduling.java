package com.weaver.emobile.gateway.scheduling;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaver.emobile.gateway.util.RouteUtils;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

@Component
public class GatewayScheduling {
    private static final String FETCH_URL = "http://127.0.0.1:8080/routes";

    private static Logger logger = LoggerFactory.getLogger(GatewayScheduling.class);

    private WebClient webClient;

    public GatewayScheduling() {
        webClient = WebClient.builder().build();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void refreshRoutes() {
        Mono<String> mono = this.webClient.get()
                .uri(FETCH_URL)
                .httpRequest(httpRequest  -> {
                    HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
                    reactorRequest.responseTimeout(Duration.ofSeconds(2));
                })
                .retrieve()
                .bodyToMono(String.class);
        mono.subscribe(this::handleRefreshRoutesResp);
    }

    private void handleRefreshRoutesResp(String body) {
        try {
            Map<String, String> routes = new ObjectMapper().readValue(body, new TypeReference<Map<String, String>>() {});
            RouteUtils.setRoutes(routes);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
