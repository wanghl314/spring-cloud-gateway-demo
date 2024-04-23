package com.weaver.emobile.gateway.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.net.Socket;
import java.util.UUID;

public class TcpMaster extends AbstractMaster {
    private static Logger logger = LoggerFactory.getLogger(TcpMaster.class);

    private final String host;

    private final int port;

    public TcpMaster(int serverPort, String host, int port) {
        super(serverPort, TcpWorker.builder().host(host).port(port));
        this.host = host;
        this.port = port;
    }

    public TcpMaster(String serverName, int serverPort, String host, int port) {
        super(serverName, serverPort, TcpWorker.builder().host(host).port(port));
        this.host = host;
        this.port = port;
    }

    public TcpMaster(String serverName, String serverAddress, int serverPort, String host, int port) {
        super(serverName, serverAddress, serverPort, TcpWorker.builder().host(host).port(port));
        this.host = host;
        this.port = port;
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

                Socket proxy = SocketFactory.getDefault().createSocket(this.host, this.port);
                String workerName = UUID.randomUUID().toString();
                Worker request = TcpWorker.builder()
                        .workerName(workerName)
                        .mode(TcpWorker.Mode.REQUEST)
                        .client(client)
                        .proxy(proxy)
                        .build();
                Worker response = TcpWorker.builder()
                        .workerName(workerName)
                        .mode(TcpWorker.Mode.RESPONSE)
                        .client(proxy)
                        .proxy(client)
                        .build();
                this.taskExecutor.submit(request);
                this.taskExecutor.submit(response);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
