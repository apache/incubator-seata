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
package io.seata.codec.seata.protocol.transaction;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.seata.core.protocol.transaction.AbstractGlobalEndRequest;

/**
 * The type Abstract global end request codec.
 *
 * @author zhangsen
 */
public abstract class AbstractGlobalEndRequestCodec extends AbstractTransactionRequestToTCCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractGlobalEndRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        AbstractGlobalEndRequest abstractGlobalEndRequest = (AbstractGlobalEndRequest)t;
        String xid = abstractGlobalEndRequest.getXid();
        String extraData = abstractGlobalEndRequest.getExtraData();

        // 1. xid
        if (xid != null) {
            byte[] bs = xid.getBytes(UTF8);
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
    public <T> void decode(T t, ByteBuffer in) {
        AbstractGlobalEndRequest abstractGlobalEndRequest = (AbstractGlobalEndRequest)t;

        short xidLen = in.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            in.get(bs);
            abstractGlobalEndRequest.setXid(new String(bs, UTF8));
        }
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            abstractGlobalEndRequest.setExtraData(new String(bs, UTF8));
        }
    }

}
