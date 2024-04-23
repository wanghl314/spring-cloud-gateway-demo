package com.weaver.emobile.gateway.consts;

import java.util.regex.Pattern;

public interface TcpConsts {
    int EXCHANGE_KEY = 1;

    int EXCHANGE_DATA = 2;

    String SUCCESS = "OK";

    String INVALID_KEY = "INVALID KEY";

    String KEY_NOT_EXCHANGED = "KEY NOT EXCHANGED";

    String SUFFIX = "\r";

    Pattern PATTERN = Pattern.compile("^(" + EXCHANGE_KEY + "|" + EXCHANGE_DATA + ") (\\d) (\\w+) (\\w+) (.+?)" + SUFFIX + "$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
}
