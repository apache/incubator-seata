/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.seata.protocol.v2;

import io.netty.buffer.ByteBuf;
import org.apache.seata.core.protocol.AbstractIdentifyResponse;
import org.apache.seata.serializer.seata.protocol.AbstractResultMessageCodec;

import java.nio.ByteBuffer;

/**
 *  The type Abstract identify request codec.(v2)
 **/
public class AbstractIdentifyResponseCodecV2 extends AbstractResultMessageCodec {
    @Override
    public Class<?> getMessageClassType() {
        return AbstractIdentifyResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);
        AbstractIdentifyResponse abstractIdentifyResponse = (AbstractIdentifyResponse) t;
        boolean identified = abstractIdentifyResponse.isIdentified();
        String version = abstractIdentifyResponse.getVersion();

        out.writeByte(identified ? (byte) 1 : (byte) 0);
        if (version != null) {
            byte[] bs = version.getBytes(UTF8);
            out.writeShort((short) bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short) 0);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        AbstractIdentifyResponse abstractIdentifyResponse = (AbstractIdentifyResponse) t;
        super.decode(t, in);
        abstractIdentifyResponse.setIdentified(in.get() == 1);
        short len = in.getShort();
        if (len <= 0) {
            return;
        }
        if (in.remaining() < len) {
            return;
        }
        byte[] bs = new byte[len];
        in.get(bs);
        abstractIdentifyResponse.setVersion(new String(bs, UTF8));
    }
}
