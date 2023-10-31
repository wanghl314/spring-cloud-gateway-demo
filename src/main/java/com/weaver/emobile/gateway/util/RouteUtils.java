package com.weaver.emobile.gateway.util;

import java.util.HashMap;
import java.util.Map;

import com.weaver.emobile.gateway.scheduling.GatewayScheduling;

public class RouteUtils {
    private static Map<String, String> routes = new HashMap<String, String>();

    public static Map<String, String> getRoutes() {
        return RouteUtils.routes;
    }

    public static void setRoutes(Map<String, String> routes) {
        if (routes != null && !routes.isEmpty()) {
            synchronized (GatewayScheduling.class) {
                RouteUtils.routes = routes;
            }
        }
    }

}
