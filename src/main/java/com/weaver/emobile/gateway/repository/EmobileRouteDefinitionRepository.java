package com.weaver.emobile.gateway.repository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class EmobileRouteDefinitionRepository implements RouteDefinitionRepository {

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> routes = new ArrayList<RouteDefinition>();

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        Map<String, String> predicateParams = new HashMap<String, String>();
        predicateParams.put("pattern", "/**");
        predicate.setArgs(predicateParams);

        URI uri = UriComponentsBuilder.fromHttpUrl("http://192.168.1.241:8083").build().toUri();
//        URI uri = UriComponentsBuilder.fromHttpUrl("http://192.168.1.243:8088").build().toUri();

        RouteDefinition route = new RouteDefinition();
        route.setId("test");
        route.setPredicates(Arrays.asList(predicate));
        route.setUri(uri);
        routes.add(route);
        return Flux.fromIterable(routes);
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return null;
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return null;
    }

}
