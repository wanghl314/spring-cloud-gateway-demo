package com.weaver.emobile.gateway.tcp.xmpp;

import com.weaver.emobile.gateway.consts.GatewayConsts;
import com.weaver.emobile.gateway.consts.HttpConsts;
import com.weaver.emobile.gateway.consts.TcpConsts;
import com.weaver.emobile.gateway.tcp.AbstractWorker;
import com.weaver.emobile.gateway.tcp.Worker;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;

public class XMPPWorker extends AbstractWorker {
    private static Logger logger = LoggerFactory.getLogger(XMPPWorker.class);

    private String key;

    private IoConnector connector;

    private IoSession session;

    private XMPPWorker(String host, int port, Socket pull) {
        super(host, port, pull);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void doRun() throws Exception {
        try (InputStream in = new BufferedInputStream(this.client.getInputStream());
             OutputStream out = new BufferedOutputStream(this.client.getOutputStream())) {
            outer: while (true) {
                byte[] buffer;

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    // 从输入流中循环读取并缓存数据，直到读取到结束标识或缓冲区容量达到上限
                    while (true) {
                        byte[] temp = new byte[1024];
                        int length = in.read(temp);

                        if (length == -1) {
                            break outer;
                        }
                        baos.write(temp, 0, length);
                        String endFlag = TcpConsts.SUFFIX;

                        if (StringUtils.endsWith(baos.toString(StandardCharsets.UTF_8), endFlag)) {
                            break;
                        }

                        if (baos.size() > MAX_BUFFER_SIZE.toBytes()) {
                            break outer;
                        }
                    }
                    buffer = baos.toByteArray();
                }
                Matcher matcher = TcpConsts.PATTERN.matcher(new String(buffer, StandardCharsets.UTF_8));

                if (matcher.matches()) {
                    String exchange = matcher.group(1);
                    String version = matcher.group(2);
                    String keyAlgorithm = matcher.group(3);
                    String dataAlgorithm = matcher.group(4);
                    String data = matcher.group(5);

                    if (logger.isDebugEnabled()) {
                        logger.debug("request >> exchange: {}", exchange);
                        logger.debug("request >> version: {}", version);
                        logger.debug("request >> keyAlgorithm: {}", keyAlgorithm);
                        logger.debug("request >> dataAlgorithm: {}", dataAlgorithm);
                        logger.debug("request >> data: {}", data);
                    }

                    if (StringUtils.equals(TcpConsts.EXCHANGE_KEY + "", exchange)) {
                        String key = new String(Base64.decodeBase64(data), StandardCharsets.UTF_8);
                        String message = TcpConsts.INVALID_KEY;

                        if (StringUtils.isBlank(key)) {
                            out.write(message.getBytes(StandardCharsets.UTF_8));
                            out.write(TcpConsts.SUFFIX.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                            continue;
                        }
                        message = TcpConsts.SUCCESS;
                        Cipher cipher = null;

                        try {
                            cipher = this.getEncryptCipher(GatewayConsts.Algorithm.AES, key);
                        } catch (InvalidKeyException e) {
                            message = TcpConsts.INVALID_KEY;
                        }

                        if (cipher != null) {
                            this.key = key;
                        }

                        if (this.connector == null) {
                            this.connector = this.getConnector(this.host, this.port);
                            this.connector.setHandler(new XMPPIoHandler() {

                                @Override
                                public void messageReceived(IoSession session, Object message) throws Exception {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("proxy-response << {}", message);
                                    }
                                    // 临时采用base64编码
                                    byte[] bytes = Base64.encodeBase64(message.toString().getBytes(StandardCharsets.UTF_8));
                                    // TODO 加密返回key交换结果
//                                    byte[] bytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
                                    out.write(bytes);
                                    out.write(TcpConsts.SUFFIX.getBytes(StandardCharsets.UTF_8));
                                    out.flush();
                                }

                                @Override
                                public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                                    logger.error(cause.getMessage(), cause);
                                }

                            });
                            ConnectFuture connectFuture = this.connector.connect();
                            connectFuture.awaitUninterruptibly();
                            this.session = connectFuture.getSession();
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("response << {}", message);
                        }
                        byte[] bytes;

                        if (cipher != null) {
                            // 临时采用base64编码
                            bytes = Base64.encodeBase64(message.getBytes(StandardCharsets.UTF_8));
                            // TODO 加密返回key交换结果
//                            bytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
                        } else {
                            bytes = message.getBytes(StandardCharsets.UTF_8);
                        }
                        out.write(bytes);
                        out.write(TcpConsts.SUFFIX.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        continue;
                    } else if (StringUtils.isBlank(this.key)) {
                        String message = TcpConsts.KEY_NOT_EXCHANGED;

                        if (logger.isDebugEnabled()) {
                            logger.debug("response << {}", message);
                        }
                        out.write(message.getBytes(StandardCharsets.UTF_8));
                        out.write(TcpConsts.SUFFIX.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        continue;
                    } else {
                        // TODO 请求数据解密处理
                        buffer = Base64.decodeBase64(data.getBytes(StandardCharsets.UTF_8));

                        if (logger.isDebugEnabled()) {
                            logger.debug("proxy-request >> {}", new String(buffer, StandardCharsets.UTF_8));
                        }
                    }
                }
                // 向代理服务写数据
                this.session.write(buffer);
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.session != null) {
            this.session.closeNow();
        }
        if (this.connector != null) {
            this.connector.dispose();
        }
    }

    private Cipher getEncryptCipher(String algorithm, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = null;

        switch (StringUtils.upperCase(algorithm)) {
            case GatewayConsts.Algorithm.RSA:
                break;
            case GatewayConsts.Algorithm.SM2:
                break;
            case GatewayConsts.Algorithm.AES:
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                break;
            case GatewayConsts.Algorithm.SM4:
                break;
        }
        return cipher;
    }

    private IoConnector getConnector(String host, int port) {
        IoConnector connector = new NioSocketConnector();
        connector.setDefaultRemoteAddress(new InetSocketAddress(host, port));
        connector.setConnectTimeoutMillis(HttpConsts.CONNECT_TIMEOUT.toMillis());

        LoggingFilter loggingFilter = new LoggingFilter();
        loggingFilter.setSessionCreatedLogLevel(LogLevel.DEBUG);
        loggingFilter.setSessionOpenedLogLevel(LogLevel.DEBUG);
        loggingFilter.setSessionClosedLogLevel(LogLevel.DEBUG);
        loggingFilter.setSessionIdleLogLevel(LogLevel.DEBUG);
        loggingFilter.setMessageReceivedLogLevel(LogLevel.DEBUG);
        loggingFilter.setMessageSentLogLevel(LogLevel.DEBUG);
        connector.getFilterChain().addLast("logger", loggingFilter);
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new XMPPCodecFactory()));
        return connector;
    }

    public static class Builder extends Worker.AbstractBuilder {
        private Socket client;

        private Builder() {
            super();
        }

        public Builder client(Socket client) {
            this.client = client;
            return this;
        }

        @Override
        public XMPPWorker build() {
            return new XMPPWorker(this.host, this.port, this.client);
        }

    }

}
