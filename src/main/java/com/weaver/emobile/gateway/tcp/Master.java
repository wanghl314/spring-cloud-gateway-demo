package com.weaver.emobile.gateway.tcp;

import org.springframework.beans.factory.DisposableBean;

public interface Master extends Runnable, DisposableBean {
    String getServerName();

    String getServerAddress();

    int getServerPort();

    void doRun();

    Worker.Builder getBuilder();
}
