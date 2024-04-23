package com.weaver.emobile.gateway.tcp;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class TcpWorker extends AbstractWorker {
    private static Logger logger = LoggerFactory.getLogger(TcpWorker.class);

    private final String workerName;

    private final Mode mode;

    private final Socket proxy;

    private TcpWorker(String workerName, Mode mode, Socket client, Socket proxy) {
        super(null, 0, client);
        this.workerName = workerName;
        this.mode = mode;
        this.proxy = proxy;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void doRun() throws IOException {
        try (InputStream in = new BufferedInputStream(this.client.getInputStream());
             OutputStream out = new BufferedOutputStream(this.proxy.getOutputStream())) {
            while (true) {
                byte[] buffer = new byte[1024];
                int length = in.read(buffer);

                if (length == -1) {
                    break;
                }
                out.write(buffer, 0, length);
                out.flush();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.proxy != null) {
            this.proxy.close();
        }
    }

    public enum Mode {
        REQUEST, RESPONSE;
    }

    @Getter
    public static class Builder extends Worker.AbstractBuilder {
        private String workerName;

        private Mode mode;

        private Socket client;

        private Socket proxy;

        private Builder() {
            super();
        }

        public Builder workerName(String workerName) {
            this.workerName = workerName;
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder client(Socket client) {
            this.client = client;
            return this;
        }

        public Builder proxy(Socket proxy) {
            this.proxy = proxy;
            return this;
        }

        @Override
        public TcpWorker build() {
            return new TcpWorker(this.workerName, this.mode, this.client, this.proxy);
        }

    }

}
