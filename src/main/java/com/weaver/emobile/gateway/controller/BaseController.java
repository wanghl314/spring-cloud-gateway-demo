package com.weaver.emobile.gateway.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weaver.emobile.gateway.util.Consts;

@RestController
@RequestMapping("/base")
public class BaseController {

    @GetMapping("/getsetting")
    public Map<String, String> getSetting() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("keyEncryptType", "RSA");
        data.put("dataEncryptType", "AES");
        data.put("keyEncryptKey", Consts.PUBLIC_KEY);
        return data;
    }

}
