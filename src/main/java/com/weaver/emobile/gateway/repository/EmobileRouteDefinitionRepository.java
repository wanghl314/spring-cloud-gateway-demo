package com.weaver.emobile.gateway.repository;

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmobileRouteDefinitionRepository implements RouteDefinitionRepository {

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> routes = new ArrayList<RouteDefinition>();

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        Map<String, String> predicateArgs = new HashMap<String, String>();
        predicateArgs.put("pattern", "/spring/**");
        predicate.setArgs(predicateArgs);

        FilterDefinition filter = new FilterDefinition();
        filter.setName("StripPrefix");
        Map<String, String> filterArgs = new HashMap<String, String>();
        filterArgs.put(StripPrefixGatewayFilterFactory.PARTS_KEY, "0");
        filter.setArgs(filterArgs);

        RouteDefinition route = new RouteDefinition();
        route.setId("spring-demo");
        route.setPredicates(List.of(predicate));
        route.setFilters(List.of(filter));
        route.setUri(UriComponentsBuilder.fromHttpUrl("http://127.0.0.1:8080/spring").build().toUri());
        route.setOrder(1);
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
