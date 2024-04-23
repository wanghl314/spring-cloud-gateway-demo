package com.weaver.emobile.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = TcpProxyProperties.PREFIX)
public class TcpProxyProperties {
    public static final String PREFIX = "weaver.em.tcp-proxy";

    private List<Server> servers = new ArrayList<Server>();

    @Getter
    @Setter
    public static class Server {
        private Protocol protocol = Protocol.TCP;

        private String serverName;

        private String localAddress;

        private int localPort;

        private String targetHost;

        private int targetPort;
    }

    public enum Protocol {
        TCP,
        XMPP;
    }

}
