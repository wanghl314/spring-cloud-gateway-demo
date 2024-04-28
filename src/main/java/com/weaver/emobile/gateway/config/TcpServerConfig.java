package com.weaver.emobile.gateway.config;

import com.weaver.emobile.gateway.tcp.Master;
import com.weaver.emobile.gateway.tcp.TcpMaster;
import com.weaver.emobile.gateway.tcp.Worker;
import com.weaver.emobile.gateway.tcp.xmpp.XMPPMaster;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties({TcpProxyProperties.class, SecurityTransferProperties.class})
public class TcpServerConfig {
    private static Logger logger = LoggerFactory.getLogger(TcpServerConfig.class);

    private final TcpProxyProperties tcpProxyProperties;

    private final SecurityTransferProperties securityTransferProperties;

    private final List<Master> masters;

    public TcpServerConfig(TcpProxyProperties tcpProxyProperties,
                           SecurityTransferProperties securityTransferProperties,
                           ObjectProvider<List<Master>> provider) {
        this.tcpProxyProperties = tcpProxyProperties;
        this.securityTransferProperties = securityTransferProperties;
        this.masters = new ArrayList<Master>();

        if (provider.getIfAvailable() != null) {
            this.masters.addAll(provider.getIfAvailable());
        }
    }

    @PostConstruct
    public void init() {
        List<TcpProxyProperties.Server> servers = new ArrayList<TcpProxyProperties.Server>();

        if (CollectionUtils.isNotEmpty(this.tcpProxyProperties.getServers())) {
            servers.addAll(this.tcpProxyProperties.getServers().stream()
                    .filter(server -> server.getProtocol() == TcpProxyProperties.Protocol.TCP)
                    .toList());
        }

        if (this.securityTransferProperties.isEnabled() &&
                CollectionUtils.isNotEmpty(this.securityTransferProperties.getTcpServers())) {
            servers.addAll(this.securityTransferProperties.getTcpServers().stream()
                    .filter(server -> server.getProtocol() == TcpProxyProperties.Protocol.XMPP)
                    .toList());
        }

        if (CollectionUtils.isNotEmpty(servers)) {
            for (TcpProxyProperties.Server server: servers) {
                String serverName = server.getServerName();
                String localAddress = server.getLocalAddress();
                int localPort = server.getLocalPort();
                String targetHost = server.getTargetHost();
                int targetPort = server.getTargetPort();

                if (server.getProtocol() == TcpProxyProperties.Protocol.XMPP) {
                    this.masters.add(new XMPPMaster(serverName, localAddress, localPort, targetHost, targetPort));
                } else {
                    this.masters.add(new TcpMaster(serverName, localAddress, localPort, targetHost, targetPort));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(this.masters)) {
            for (Master master : this.masters) {
                String serverName = master.getServerName();
                String serverAddress = master.getServerAddress();
                int serverPort = master.getServerPort();
                Worker.Builder builder = master.getBuilder();

                Thread thread = new Thread(master);
                thread.setName(serverName);
                thread.start();

                logger.info("TcpServer {} started on {}:{}, proxy {}", serverName, serverAddress, serverPort, builder.toString());
            }
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (CollectionUtils.isNotEmpty(this.masters)) {
            for (Master master : this.masters) {
                master.destroy();
            }
        }
    }

}
