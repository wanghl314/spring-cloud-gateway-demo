package com.weaver.emobile.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.weaver.emobile.gateway.scheduling.GatewayScheduling;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Autowired
    private GatewayScheduling scheduling;

    @PostConstruct
    public void init() {
        this.scheduling.refreshRoutes();
    }

}
