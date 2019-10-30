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

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.seata.core.protocol.AbstractIdentifyRequest;

/**
 * The type Abstract identify request codec.
 */
public abstract class AbstractIdentifyRequestCodec extends AbstractMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractIdentifyRequest.class;
    }

    /**
     * Do encode.
     *
     * @param <T> the type parameter
     * @param t   the t
     * @param out the out
     */
    protected <T> void doEncode(T t, ByteBuf out) {
        AbstractIdentifyRequest abstractIdentifyRequest = (AbstractIdentifyRequest)t;
        String version = abstractIdentifyRequest.getVersion();
        String applicationId = abstractIdentifyRequest.getApplicationId();
        String transactionServiceGroup = abstractIdentifyRequest.getTransactionServiceGroup();
        String extraData = abstractIdentifyRequest.getExtraData();

        if (version != null) {
            byte[] bs = version.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }

        if (applicationId != null) {
            byte[] bs = applicationId.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }

        if (transactionServiceGroup != null) {
            byte[] bs = transactionServiceGroup.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }

        if (extraData != null) {
            byte[] bs = extraData.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }

    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        doEncode(t, out);
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        AbstractIdentifyRequest abstractIdentifyRequest = (AbstractIdentifyRequest)t;

        //version len
        short len = 0;
        if (in.remaining() < 2) {
            return;
        }
        len = in.getShort();
        //version
        if (in.remaining() < len) {
            return;
        }
        byte[] bs = new byte[len];
        in.get(bs);
        abstractIdentifyRequest.setVersion(new String(bs, UTF8));

        //applicationId len
        if (in.remaining() < 2) {
            return;
        }
        len = in.getShort();
        //applicationId
        if (in.remaining() < len) {
            return;
        }
        bs = new byte[len];
        in.get(bs);
        abstractIdentifyRequest.setApplicationId(new String(bs, UTF8));

        //transactionServiceGroup len
        if (in.remaining() < 2) {
            return;
        }
        len = in.getShort();

        //transactionServiceGroup
        if (in.remaining() < len) {
            return;
        }
        bs = new byte[len];
        in.get(bs);
        abstractIdentifyRequest.setTransactionServiceGroup(new String(bs, UTF8));

        //ExtraData len
        if (in.remaining() < 2) {
            return;
        }
        len = in.getShort();

        if (in.remaining() >= len) {
            bs = new byte[len];
            in.get(bs);
            abstractIdentifyRequest.setExtraData(new String(bs, UTF8));
        } else {
            //maybe null
        }
    }

}
