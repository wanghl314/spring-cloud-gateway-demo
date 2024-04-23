package com.weaver.emobile.gateway.tcp.xmpp;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import java.nio.charset.StandardCharsets;

public abstract class XMPPIoHandler extends IoHandlerAdapter {

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        XMLLightweightParser parser = new XMLLightweightParser(StandardCharsets.UTF_8);
        session.setAttribute(XMPPCodecFactory.XML_PARSER, parser);
    }

}
