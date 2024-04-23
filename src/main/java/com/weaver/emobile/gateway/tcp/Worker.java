package com.weaver.emobile.gateway.tcp;

import org.springframework.util.unit.DataSize;

public interface Worker extends Runnable {
    DataSize MAX_BUFFER_SIZE = DataSize.ofKilobytes(64);

    void doRun() throws Exception;

    interface Builder {
        Builder host(String host);

        Builder port(int port);

        Worker build();
    }

    abstract class AbstractBuilder implements Builder {
        protected String host;

        protected int port;

        protected AbstractBuilder() {
        }

        @Override
        public AbstractBuilder host(String host) {
            this.host = host;
            return this;
        }

        @Override
        public AbstractBuilder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public String toString() {
            return this.host + ":" + this.port;
        }
    }
}
