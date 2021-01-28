package com.weaver.emobile.gateway.filter;

import static java.util.function.Function.identity;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyEncoder;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaver.emobile.gateway.global.BodyEncryptException;
import com.weaver.emobile.gateway.util.Consts;
import com.weaver.emobile.gateway.util.EncodeUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ResponseEncryptFilter implements GlobalFilter, Ordered {
    private static Logger logger = LoggerFactory.getLogger(ResponseEncryptFilter.class);

    private final Map<String, MessageBodyDecoder> messageBodyDecoders;

    private final Map<String, MessageBodyEncoder> messageBodyEncoders;

    public ResponseEncryptFilter(Set<MessageBodyDecoder> messageBodyDecoders,
                                 Set<MessageBodyEncoder> messageBodyEncoders) {
        this.messageBodyDecoders = messageBodyDecoders.stream()
                .collect(Collectors.toMap(MessageBodyDecoder::encodingType, identity()));
        this.messageBodyEncoders = messageBodyEncoders.stream()
                .collect(Collectors.toMap(MessageBodyEncoder::encodingType, identity()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange.mutate().response(decorate(exchange)).build());
    }

    ServerHttpResponse decorate(ServerWebExchange exchange) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                String originalResponseContentType = exchange
                        .getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);

                if (StringUtils.isNotBlank(originalResponseContentType) &&
                        originalResponseContentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);

                    ClientResponse clientResponse = prepareClientResponse(body, httpHeaders);

                    Mono modifiedBody = extractBody(exchange, clientResponse, String.class)
                            .flatMap(originalBody -> {
                                String newBody = null;
                                String path = exchange.getRequest().getPath().toString();

                                if ("/emp/passport/getsetting".equalsIgnoreCase(path)) {
                                    try {
                                        Map<String, Object> encryptOption = new HashMap<String, Object>();
                                        encryptOption.put("keyEncryptType", "RSA");
                                        encryptOption.put("keyEncryptKey", Consts.PUBLIC_KEY);
                                        encryptOption.put("dataEncryptType", "AES");

                                        ObjectMapper mapper = new ObjectMapper();
                                        Map<String, Object> returnValue = mapper.readValue(originalBody, Map.class);
                                        returnValue.put("encryptOption", encryptOption);
                                        newBody = mapper.writeValueAsString(returnValue);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    String dataEncryptDecryptKey = exchange.getAttribute("Decrypted-Data-Encrypt-Key");

                                    if (StringUtils.isNotBlank(dataEncryptDecryptKey) && StringUtils.isNotBlank(originalBody)) {
                                        try {
                                            newBody = EncodeUtils.aesEncrypt(originalBody, dataEncryptDecryptKey);
                                        } catch (Exception e) {
                                            throw new BodyEncryptException(e);
                                        }
                                    } else{
                                        newBody = originalBody;
                                    }
                                }
                                return Mono.just(newBody);
                            });

                    BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody,
                            String.class);
                    CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
                            exchange.getResponse().getHeaders());
                    return bodyInserter.insert(outputMessage, new BodyInserterContext())
                            .then(Mono.defer(() -> {
                                Mono<DataBuffer> messageBody = writeBody(getDelegate(),
                                        outputMessage, String.class);
                                HttpHeaders headers = getDelegate().getHeaders();
                                if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
                                        || headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                                    messageBody = messageBody.doOnNext(data -> headers
                                            .setContentLength(data.readableByteCount()));
                                }
                                // TODO: fail if isStreamingMediaType?
                                return getDelegate().writeWith(messageBody);
                            }));
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(
                    Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }

            private ClientResponse prepareClientResponse(Publisher<? extends DataBuffer> body,
                                                         HttpHeaders httpHeaders) {
                ClientResponse.Builder builder;
                builder = ClientResponse.create(exchange.getResponse().getStatusCode());
                return builder.headers(headers -> headers.putAll(httpHeaders))
                        .body(Flux.from(body)).build();
            }

            private <T> Mono<T> extractBody(ServerWebExchange exchange,
                                            ClientResponse clientResponse, Class<T> inClass) {
                // if inClass is byte[] then just return body, otherwise check if
                // decoding required
                if (byte[].class.isAssignableFrom(inClass)) {
                    return clientResponse.bodyToMono(inClass);
                }

                List<String> encodingHeaders = exchange.getResponse().getHeaders()
                        .getOrEmpty(HttpHeaders.CONTENT_ENCODING);
                for (String encoding : encodingHeaders) {
                    MessageBodyDecoder decoder = messageBodyDecoders.get(encoding);
                    if (decoder != null) {
                        return clientResponse.bodyToMono(byte[].class)
                                .publishOn(Schedulers.parallel()).map(decoder::decode)
                                .map(bytes -> exchange.getResponse().bufferFactory()
                                        .wrap(bytes))
                                .map(buffer -> prepareClientResponse(Mono.just(buffer),
                                        exchange.getResponse().getHeaders()))
                                .flatMap(response -> response.bodyToMono(inClass));
                    }
                }

                return clientResponse.bodyToMono(inClass);
            }

            private Mono<DataBuffer> writeBody(ServerHttpResponse httpResponse,
                                               CachedBodyOutputMessage message, Class<?> outClass) {
                Mono<DataBuffer> response = DataBufferUtils.join(message.getBody());
                if (byte[].class.isAssignableFrom(outClass)) {
                    return response;
                }

                List<String> encodingHeaders = httpResponse.getHeaders()
                        .getOrEmpty(HttpHeaders.CONTENT_ENCODING);
                for (String encoding : encodingHeaders) {
                    MessageBodyEncoder encoder = messageBodyEncoders.get(encoding);
                    if (encoder != null) {
                        DataBufferFactory dataBufferFactory = httpResponse.bufferFactory();
                        response = response.publishOn(Schedulers.parallel()).map(buffer -> {
                            byte[] encodedResponse = encoder.encode(buffer);
                            DataBufferUtils.release(buffer);
                            return encodedResponse;
                        }).map(dataBufferFactory::wrap);
                        break;
                    }
                }

                return response;
            }

        };
    }

    @Override
    public int getOrder() {
        return -10;
    }

}
