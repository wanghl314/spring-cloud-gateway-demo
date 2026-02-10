package com.weaver.emobile.gateway.scheduling;

import com.weaver.emobile.gateway.util.RouteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
public class GatewayScheduling {
    private static final String BASE_URL = "http://127.0.0.1:8080";

    private static final String FETCH_URI = "/routes";

    private static Logger logger = LoggerFactory.getLogger(GatewayScheduling.class);

    private WebClient webClient;

    private ObjectMapper mapper;

    public GatewayScheduling(WebClient.Builder builder, ObjectMapper mapper) {
        this.webClient = builder.baseUrl(BASE_URL).build();
        this.mapper = mapper;
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void refreshRoutes() {
        Mono<String> mono = this.webClient.get()
                .uri(FETCH_URI)
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
            RouteUtils.setRoutes(this.mapper.readValue(body, new TypeReference<>() {}));
        } catch (JacksonException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
