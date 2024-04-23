package com.weaver.emobile.gateway.tcp.xmpp;

import com.weaver.emobile.gateway.tcp.AbstractMaster;
import com.weaver.emobile.gateway.tcp.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;

public class XMPPMaster extends AbstractMaster {
    private static Logger logger = LoggerFactory.getLogger(XMPPMaster.class);

    public XMPPMaster(int serverPort, String host, int port) {
        super(serverPort, XMPPWorker.builder().host(host).port(port));
    }

    public XMPPMaster(String serverName, int serverPort, String host, int port) {
        super(serverName, serverPort, XMPPWorker.builder().host(host).port(port));
    }

    public XMPPMaster(String serverName, String serverAddress, int serverPort, String host, int port) {
        super(serverName, serverAddress, serverPort, XMPPWorker.builder().host(host).port(port));
    }

    @Override
    public void doRun() {
        if (this.isStart) {
            return;
        }
        this.taskExecutor = this.getThreadPoolTaskExecutor();

        try {
            this.serverSocket = this.createServerSocket();
            this.isStart = true;

            while (this.isStart) {
                Socket client = this.serverSocket.accept();

                super.setTracingContext();

                Worker worker = ((XMPPWorker.Builder) this.builder).client(client).build();
                this.taskExecutor.submit(worker);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
