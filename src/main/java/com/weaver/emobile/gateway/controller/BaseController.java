package com.weaver.emobile.gateway.controller;

import com.weaver.emobile.gateway.util.Consts;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/base")
public class BaseController {

    @GetMapping("/getsetting")
    public Map<String, String> getSetting(ServerHttpRequest request, ServerHttpResponse response) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("keyEncryptType", "RSA");
        data.put("dataEncryptType", "AES");
        data.put("keyEncryptKey", Consts.PUBLIC_KEY);
        return data;
    }

}
