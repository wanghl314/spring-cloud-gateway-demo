package com.weaver.emobile.gateway.tcp.xmpp;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class XMPPCodecFactory implements ProtocolCodecFactory {
    public static final String XML_PARSER = "XML-PARSER";

    private final XMPPEncoder encoder;

    private final XMPPDecoder decoder;

    public XMPPCodecFactory() {
        this.encoder = new XMPPEncoder();
        this.decoder = new XMPPDecoder();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }

}
