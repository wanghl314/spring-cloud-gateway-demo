package com.weaver.emobile.gateway.config;

import com.weaver.emobile.gateway.consts.GatewayConsts;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RefreshScope
@ConfigurationProperties(prefix = SecurityTransferProperties.PREFIX)
public class SecurityTransferProperties {
    public static final String PREFIX = "weaver.em.security-transfer";

    private boolean enabled;

    private String keyAlgorithm = GatewayConsts.Algorithm.RSA;

    private String dataAlgorithm = GatewayConsts.Algorithm.AES;

    List<TcpProxyProperties.Server> tcpServers = new ArrayList<TcpProxyProperties.Server>();
}
