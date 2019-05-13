/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.codec.seata.protocol;

import io.seata.core.protocol.AbstractIdentifyResponse;

import java.nio.ByteBuffer;

/**
 * The type Abstract identify response.
 *
 * @author sharajava
 */
public abstract class AbstractIdentifyResponseCodec extends AbstractResultMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractIdentifyResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out){
        AbstractIdentifyResponse abstractIdentifyResponse = (AbstractIdentifyResponse) t;
        boolean identified = abstractIdentifyResponse.isIdentified();
        String version = abstractIdentifyResponse.getVersion();

        out.put(identified ? (byte)1 : (byte)0);
        if (version != null) {
            byte[] bs = version.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in){
        AbstractIdentifyResponse abstractIdentifyResponse = (AbstractIdentifyResponse) t;

        abstractIdentifyResponse.setIdentified(in.get() == 1);
        short len = in.getShort();
        if (len <= 0) {
            return ;
        }
        if (in.remaining() < len) {
            return ;
        }
        byte[] bs = new byte[len];
        in.get(bs);
        abstractIdentifyResponse.setVersion(new String(bs, UTF8));
    }

}
