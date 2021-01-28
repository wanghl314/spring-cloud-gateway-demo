package com.weaver.emobile.gateway.filter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

import com.weaver.emobile.gateway.global.BodyDecryptException;
import com.weaver.emobile.gateway.global.KeyDecryptException;
import com.weaver.emobile.gateway.util.Consts;
import com.weaver.emobile.gateway.util.EncodeUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RequestDecryptFilter implements GlobalFilter, Ordered {
    private static Logger logger = LoggerFactory.getLogger(RequestDecryptFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> dataEncryptKeyHeaders = requestHeaders.get("Data-Encrypt-Key");
        boolean encrypted = false;
        String dataEncryptDecryptKey = null;

        if (dataEncryptKeyHeaders != null && !dataEncryptKeyHeaders.isEmpty() && StringUtils.isNotBlank(dataEncryptKeyHeaders.get(0))) {
            try {
                dataEncryptDecryptKey = EncodeUtils.rsaDecrypt(dataEncryptKeyHeaders.get(0), Consts.PRIVATE_KEY);
                encrypted = true;
            } catch (Exception e) {
                throw new KeyDecryptException(e);
            }
        }

        if (encrypted) {
            exchange.getAttributes().put("Decrypted-Data-Encrypt-Key", dataEncryptDecryptKey);
            ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
            String finalDataEncryptDecryptKey = dataEncryptDecryptKey;

            Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                    .flatMap(originalBody -> {
                        String newBody = null;

                        if (StringUtils.isNotBlank(originalBody)) {
                            try {
                                newBody = EncodeUtils.aesDecrypt(originalBody, finalDataEncryptDecryptKey);
                            } catch (Exception e) {
                                throw new BodyDecryptException(e);
                            }
                        } else {
                            newBody = originalBody;
                        }
                        return Mono.just(newBody);
                    });
            BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(requestHeaders);
            headers.remove(HttpHeaders.CONTENT_LENGTH);

            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
            return bodyInserter.insert(outputMessage, new BodyInserterContext())
                    .then(Mono.defer(() -> {
                        ServerHttpRequest decorator = decorate(exchange, headers,
                                outputMessage);
                        return chain
                                .filter(exchange.mutate().request(decorator).build());
                    }));
        } else {
            return chain.filter(exchange);
        }
    }

    private ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers, CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {

            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(headers);

                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required' // on
                    // httpbin.org
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }

        };
    }

    @Override
    public int getOrder() {
        return -20;
    }

}
