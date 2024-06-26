package com.weaver.emobile.gateway.filter;

import com.weaver.emobile.gateway.config.SecurityTransferProperties;
import com.weaver.emobile.gateway.consts.GatewayConsts;
import com.weaver.emobile.gateway.global.BodyDecryptException;
import com.weaver.emobile.gateway.global.KeyDecryptException;
import com.weaver.emobile.gateway.util.EncodeUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
public class RequestDecryptFilter implements GlobalFilter, Ordered {
    private static Logger logger = LoggerFactory.getLogger(RequestDecryptFilter.class);

    private final ServerCodecConfigurer configurer;

    private final SecurityTransferProperties securityTransferProperties;

    public RequestDecryptFilter(ServerCodecConfigurer configurer,
                                SecurityTransferProperties securityTransferProperties) {
        this.configurer = configurer;
        this.securityTransferProperties = securityTransferProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (this.securityTransferProperties.isEnabled()) {
            HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
            MediaType contentType = requestHeaders.getContentType();
            String dataEncryptKey = requestHeaders.getFirst(GatewayConsts.REQUEST_ENCRYPT_KEY);
            String dataEncryptDecryptKey = null;

            if (StringUtils.isNotBlank(dataEncryptKey)) {
                try {
                    if (GatewayConsts.Algorithm.SM2.equalsIgnoreCase(this.securityTransferProperties.getKeyAlgorithm())) {

                    } else {
                        dataEncryptDecryptKey = EncodeUtils.rsaDecrypt(dataEncryptKey, GatewayConsts.RSA_PRIVATE_KEY);
                    }
                } catch (Exception e) {
                    throw new KeyDecryptException(e);
                }
            }

            if (StringUtils.isNotBlank(dataEncryptDecryptKey)) {
                exchange.getAttributes().put(GatewayConsts.EXCHANGE_ENCRYPT_KEY, dataEncryptDecryptKey);
                ServerRequest serverRequest = ServerRequest.create(exchange, this.configurer.getReaders());
                String finalDataEncryptDecryptKey = dataEncryptDecryptKey;

                Mono<byte[]> modifiedBody = serverRequest.bodyToMono(byte[].class)
                        .flatMap(originalBody -> {
                            byte[] newBody = originalBody;

                            if (originalBody != null) {
                                try {
                                    if (GatewayConsts.Algorithm.SM4.equalsIgnoreCase(this.securityTransferProperties.getDataAlgorithm())) {

                                    } else {
                                        SecretKeySpec secretKey = new SecretKeySpec(finalDataEncryptDecryptKey.getBytes(), "AES");
                                        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                                        cipher.init(Cipher.DECRYPT_MODE, secretKey);
                                        InputStream is;

                                        if (contentType != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
                                            is = new ByteArrayInputStream(originalBody);
                                        } else {
                                            is = new ByteArrayInputStream(Base64.decodeBase64(originalBody));
                                        }
                                        CipherInputStream cis = new CipherInputStream(is, cipher);
                                        newBody = IOUtils.toByteArray(cis);
                                    }
//                                newBody = EncodeUtils.aesDecrypt(Base64.decodeBase64(originalBody), finalDataEncryptDecryptKey);
                                } catch (Exception e) {
                                    return Mono.error(new BodyDecryptException(e));
                                }
                            }
                            return Mono.just(newBody);
                        });
                BodyInserter<Mono<byte[]>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, byte[].class);
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(requestHeaders);
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.remove(GatewayConsts.SERVER_ID);
                headers.remove(GatewayConsts.REQUEST_ENCRYPT_KEY);

                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            ServerHttpRequest decorator = decorate(exchange, headers, outputMessage);
                            return chain.filter(exchange.mutate().request(decorator).build());
                        }));
            }
        }
        return chain.filter(exchange);
    }

    private ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers,
            CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(headers);
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                }
                else {
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
        return OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 20;
    }

}
