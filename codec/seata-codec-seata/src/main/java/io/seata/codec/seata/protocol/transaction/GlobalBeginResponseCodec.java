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
import io.seata.core.protocol.transaction.GlobalBeginResponse;

/**
 * The type Global begin response codec.
 *
 * @author zhangsen
 */
public class GlobalBeginResponseCodec extends AbstractTransactionResponseCodec {

    @Override
    public Class<?> getMessageClassType() {
        return GlobalBeginResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);

        GlobalBeginResponse globalBeginResponse = (GlobalBeginResponse)t;
        String xid = globalBeginResponse.getXid();
        String extraData = globalBeginResponse.getExtraData();

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
        super.decode(t, in);

        GlobalBeginResponse globalBeginResponse = (GlobalBeginResponse)t;

        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            globalBeginResponse.setXid(new String(bs, UTF8));
        }

        len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            globalBeginResponse.setExtraData(new String(bs, UTF8));
        }
    }
}
