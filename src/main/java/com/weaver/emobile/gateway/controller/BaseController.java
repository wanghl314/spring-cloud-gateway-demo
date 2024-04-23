package com.weaver.emobile.gateway.controller;

import com.weaver.emobile.gateway.config.SecurityTransferProperties;
import com.weaver.emobile.gateway.consts.GatewayConsts;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private SecurityTransferProperties properties;

    @GetMapping("/getsetting")
    public Map<String, String> getSetting(ServerHttpRequest request, ServerHttpResponse response) {
        Map<String, String> data = new HashMap<String, String>();

        if (this.properties.isEnabled()) {
            String keyEncryptAlgorithm = this.properties.getKeyAlgorithm();
            data.put("keyEncryptAlgorithm", keyEncryptAlgorithm);
            data.put("dataEncryptAlgorithm", this.properties.getDataAlgorithm());
            String publicKey = null;

            if (GatewayConsts.Algorithm.SM2.equalsIgnoreCase(keyEncryptAlgorithm)) {
                publicKey = GatewayConsts.SM2_PUBLIC_KEY;
            } else {
                publicKey = GatewayConsts.RSA_PUBLIC_KEY;
            }
            data.put("publicKey", publicKey);
        }
        return data;
    }

}
