package com.weaver.emobile.gateway.filter;

import com.weaver.emobile.gateway.config.SecurityTransferProperties;
import com.weaver.emobile.gateway.consts.GatewayConsts;
import com.weaver.emobile.gateway.global.BodyEncryptException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
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
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

@Component
public class ResponseEncryptFilter implements GlobalFilter, Ordered {
    private static Logger logger = LoggerFactory.getLogger(ResponseEncryptFilter.class);

    private final ServerCodecConfigurer configurer;

    private final Map<String, MessageBodyDecoder> messageBodyDecoders;

    private final Map<String, MessageBodyEncoder> messageBodyEncoders;

    private final SecurityTransferProperties securityTransferProperties;

    public ResponseEncryptFilter(ServerCodecConfigurer configurer,
                                 Set<MessageBodyDecoder> messageBodyDecoders,
                                 Set<MessageBodyEncoder> messageBodyEncoders,
                                 SecurityTransferProperties securityTransferProperties) {
        this.configurer = configurer;
        this.messageBodyDecoders = messageBodyDecoders.stream()
                .collect(Collectors.toMap(MessageBodyDecoder::encodingType, identity()));
        this.messageBodyEncoders = messageBodyEncoders.stream()
                .collect(Collectors.toMap(MessageBodyEncoder::encodingType, identity()));
        this.securityTransferProperties = securityTransferProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (this.securityTransferProperties.isEnabled()) {
            return chain.filter(exchange.mutate().response(new ModifiedServerHttpResponse(exchange)).build());
        }
        return chain.filter(exchange);
    }

    private class ModifiedServerHttpResponse extends ServerHttpResponseDecorator {

        private final ServerWebExchange exchange;

        public ModifiedServerHttpResponse(ServerWebExchange exchange) {
            super(exchange.getResponse());
            this.exchange = exchange;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            String originalResponseContentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            String dataEncryptDecryptKey = exchange.getAttribute(GatewayConsts.EXCHANGE_ENCRYPT_KEY);

            if (StringUtils.isNotBlank(dataEncryptDecryptKey)) {
                boolean dataEncrypt = (StringUtils.isNotBlank(originalResponseContentType) &&
                        (MediaType.TEXT_HTML.isCompatibleWith(MediaType.parseMediaType(originalResponseContentType)) ||
                                MediaType.TEXT_PLAIN.isCompatibleWith(MediaType.parseMediaType(originalResponseContentType)) ||
                                MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(originalResponseContentType)) ||
                                MediaType.APPLICATION_XML.isCompatibleWith(MediaType.parseMediaType(originalResponseContentType))));
                this.exchange.getResponse().getHeaders().set(GatewayConsts.RESPONSE_ENCRYPT, dataEncrypt ? "1" : "0");

                if (dataEncrypt) {
                    return this.modifyBody(body, originalBody -> {
                        byte[] newBody = originalBody;

                        if (originalBody != null) {
                            try {
                                if (GatewayConsts.Algorithm.SM4.equalsIgnoreCase(securityTransferProperties.getDataAlgorithm())) {

                                } else {
                                    SecretKeySpec secretKey = new SecretKeySpec(dataEncryptDecryptKey.getBytes(), "AES");
                                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                                    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                                    CipherInputStream cis = new CipherInputStream(new ByteArrayInputStream(originalBody), cipher);
                                    byte[] bytes = IOUtils.toByteArray(cis);
                                    newBody = Base64.encodeBase64(bytes);
                                }
                            } catch (Exception e) {
                                return Mono.error(new BodyEncryptException(e));
                            }
                        }
                        return Mono.just(newBody);
                    });
                }
            }
            return super.writeWith(body);
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return writeWith(Flux.from(body).flatMapSequential(p -> p));
        }

        private Mono<Void> modifyBody(Publisher<? extends DataBuffer> body, Function<? super byte[], ? extends Mono<? extends byte[]>> transformer) {
            String originalResponseContentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            HttpHeaders httpHeaders = new HttpHeaders();
            // explicitly add it in this way instead of
            // 'httpHeaders.setContentType(originalResponseContentType)'
            // this will prevent exception in case of using non-standard media
            // types like "Content-Type: image"
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);

            ClientResponse clientResponse = prepareClientResponse(body, httpHeaders);

            // TODO: flux or mono
            Mono<byte[]> modifiedBody = extractBody(exchange, clientResponse, byte[].class)
                    .flatMap(transformer);

            BodyInserter<Mono<byte[]>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, byte[].class);
            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
                    exchange.getResponse().getHeaders());
            return bodyInserter.insert(outputMessage, new BodyInserterContext())
                    .then(Mono.defer(() -> {
                        Mono<DataBuffer> messageBody = writeBody(getDelegate(), outputMessage, byte[].class);
                        HttpHeaders headers = getDelegate().getHeaders();
                        if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
                                || headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                            messageBody = messageBody.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
                        }
                        // TODO: fail if isStreamingMediaType?
                        return getDelegate().writeWith(messageBody);
                    }));
        }

        private ClientResponse prepareClientResponse(Publisher<? extends DataBuffer> body, HttpHeaders httpHeaders) {
            ClientResponse.Builder builder;
            builder = ClientResponse.create(exchange.getResponse().getStatusCode(), configurer.getReaders());
            return builder.headers(headers -> headers.putAll(httpHeaders)).body(Flux.from(body)).build();
        }

        private <T> Mono<T> extractBody(ServerWebExchange exchange, ClientResponse clientResponse, Class<T> inClass) {
            // if inClass is byte[] then just return body, otherwise check if
            // decoding required
//            if (byte[].class.isAssignableFrom(inClass)) {
//                return clientResponse.bodyToMono(inClass);
//            }

            List<String> encodingHeaders = exchange.getResponse().getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
            for (String encoding : encodingHeaders) {
                MessageBodyDecoder decoder = messageBodyDecoders.get(encoding);
                if (decoder != null) {
                    return clientResponse.bodyToMono(byte[].class).publishOn(Schedulers.parallel()).map(decoder::decode)
                            .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                            .map(buffer -> prepareClientResponse(Mono.just(buffer),
                                    exchange.getResponse().getHeaders()))
                            .flatMap(response -> response.bodyToMono(inClass));
                }
            }

            return clientResponse.bodyToMono(inClass);
        }

        private Mono<DataBuffer> writeBody(ServerHttpResponse httpResponse, CachedBodyOutputMessage message,
                Class<?> outClass) {
            Mono<DataBuffer> response = DataBufferUtils.join(message.getBody());
//            if (byte[].class.isAssignableFrom(outClass)) {
//                return response;
//            }

            List<String> encodingHeaders = httpResponse.getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
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

    }

    @Override
    public int getOrder() {
        return OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 10;
    }

}
