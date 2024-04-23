/*
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weaver.emobile.gateway.tcp.xmpp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.nio.charset.StandardCharsets;

/**
 * Encoder that does nothing. We are already writing ByteBuffers so there is no need
 * to encode them.<p>
 *
 * This class exists as a counterpart of {@link XMPPDecoder}. Unlike that class this class does nothing.
 *
 * @author Gaston Dombiak
 */
public class XMPPEncoder extends ProtocolEncoderAdapter {

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out)
            throws Exception {
        IoBuffer buffer = IoBuffer.allocate(4096);
        buffer.setAutoExpand(true);

        if (message instanceof byte[]) {
            buffer.put((byte[]) message);
        } else {
            buffer.put(message.toString().getBytes(StandardCharsets.UTF_8));
        }
        buffer.flip();
        out.write(buffer);
    }

}
