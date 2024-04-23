package com.weaver.emobile.gateway.consts;

import java.time.Duration;

public interface HttpConsts {
    Duration CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(1);

    Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    Duration SOCKET_TIMEOUT = Duration.ofSeconds(20);
}
