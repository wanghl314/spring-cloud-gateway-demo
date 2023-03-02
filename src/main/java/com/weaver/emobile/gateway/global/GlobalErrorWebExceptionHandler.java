package com.weaver.emobile.gateway.global;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static Logger logger = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable t) {
        logger.error(t.getMessage(), t);
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(t);
        }
        HttpHeaders headers = response.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Decrypt-Response-Body", "false");

        if (t instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) t).getStatusCode());
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        int errcode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String errmsg = "unknown exception: " + t.getClass().getSimpleName();

        if (t instanceof KeyDecryptException) {
            errcode = 1;
            errmsg = "KeyDecryptException";
        } else if (t instanceof BodyDecryptException) {
            errcode = 2;
            errmsg = "BodyDecryptException";
        } else if (t instanceof BodyEncryptException) {
            errcode = 3;
            errmsg = "BodyEncryptException";
        }
        result.put("errcode", errcode);
        result.put("errmsg", errmsg);
        final int finalErrcode = errcode;
        final String finalErrmsg = errmsg;
        return response
                .writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    try {
                        return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
                    } catch (JsonProcessingException e) {
                        return bufferFactory.wrap(("{\"errcode\":\""+ finalErrcode +"\",\"errmsg\":\""+finalErrmsg+"\"}").getBytes(StandardCharsets.UTF_8));
                    }
                }));
    }

}