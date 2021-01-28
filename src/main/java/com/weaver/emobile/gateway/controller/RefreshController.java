package com.weaver.emobile.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class RefreshController {
    @Autowired
    private ApplicationEventPublisher publisher;

    @GetMapping("/refresh")
    public String refresh() throws Exception {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "SUCCESS";
    }

}
