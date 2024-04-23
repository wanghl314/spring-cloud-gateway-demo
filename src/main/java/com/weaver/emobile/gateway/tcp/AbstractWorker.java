package com.weaver.emobile.gateway.tcp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public abstract class AbstractWorker implements Worker {
    protected static Logger logger = LoggerFactory.getLogger(AbstractWorker.class);

    protected final String host;

    protected final int port;

    protected final Socket client;

    protected AbstractWorker(String host, int port, Socket client) {
        this.host = host;
        this.port = port;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            this.doRun();
        } catch (Exception e) {
            this.logger(e);
        } finally {
            try {
                this.close();
            } catch (IOException ignored) {
            }
        }
    }

    protected void logger(Exception e) {
        final String[] search = new String[] { "Socket closed", "Socket is closed", "Connection reset" };

        if (!(e instanceof SocketException && StringUtils.containsAnyIgnoreCase(e.getMessage(), search))) {
            logger.error(e.getMessage(), e);
        }
    }

    protected void close() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
    }

}
