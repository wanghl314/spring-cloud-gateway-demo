package com.weaver.emobile.gateway.tcp;

import brave.baggage.BaggageFields;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMaster implements Master {
    @Getter
    protected final String serverName;

    @Getter
    protected final String serverAddress;

    @Getter
    protected final int serverPort;

    @Getter
    protected final Worker.Builder builder;

    protected volatile boolean isStart;

    protected ServerSocket serverSocket;

    protected ThreadPoolTaskExecutor taskExecutor;

    protected AbstractMaster(int serverPort, Worker.Builder builder) {
        this(UUID.randomUUID().toString(), serverPort, builder);
    }

    protected AbstractMaster(String serverName, int serverPort, Worker.Builder builder) {
        this.serverName = serverName;
        this.serverAddress = "0.0.0.0";
        this.serverPort = serverPort;
        this.builder = builder;
    }

    protected AbstractMaster(String serverName, String serverAddress, int serverPort, Worker.Builder builder) {
        this.serverName = serverName;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.builder = builder;
    }

    protected ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        return this.getThreadPoolTaskExecutor(Runtime.getRuntime().availableProcessors());
    }

    protected ThreadPoolTaskExecutor getThreadPoolTaskExecutor(int coreProcessors) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(100);
        taskExecutor.setMaxPoolSize(Integer.MAX_VALUE);
        taskExecutor.setKeepAliveSeconds(60);
        taskExecutor.setQueueCapacity(coreProcessors);
        taskExecutor.setDaemon(true);
        taskExecutor.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger atomic = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName(serverName + "-worker-" + this.atomic.getAndIncrement());
                return thread;
            }

        });
        taskExecutor.setTaskDecorator(runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    MDC.setContextMap(contextMap);

                    if (MapUtils.isNotEmpty(contextMap)) {
                        MDC.put(BaggageFields.SPAN_ID.name(), UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        taskExecutor.initialize();
        return taskExecutor;
    }

    protected ServerSocket createServerSocket() throws IOException {
        ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();

        if (StringUtils.isNotBlank(this.serverAddress)) {
            return socketFactory.createServerSocket(this.serverPort, 50, InetAddress.getByName(this.serverAddress));
        }
        return socketFactory.createServerSocket(this.serverPort);
    }

    @Override
    public void run() {
        this.doRun();
    }

    @Override
    public void destroy() throws Exception {
        if (this.serverSocket != null) {
            this.serverSocket.close();
        }
        if (this.taskExecutor != null) {
            this.taskExecutor.shutdown();
        }
        this.isStart = false;
    }

    protected void setTracingContext() {
        MDC.put(BaggageFields.TRACE_ID.name(), UUID.randomUUID().toString().replace("-", ""));
        MDC.put(BaggageFields.SPAN_ID.name(), UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

}
