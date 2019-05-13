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

import io.seata.core.protocol.transaction.GlobalBeginRequest;

import java.nio.ByteBuffer;

/**
 * The type Global begin request codec.
 *
 * @author zhangsen
 */
public class GlobalBeginRequestCodec extends AbstractTransactionRequestToTCCodec {

    @Override
    public Class<?> getMessageClassType() {
        return GlobalBeginRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out) {
        GlobalBeginRequest globalBeginRequest = (GlobalBeginRequest) t;
        int timeout = globalBeginRequest.getTimeout();
        String transactionName = globalBeginRequest.getTransactionName();

        out.putInt(timeout);
        if (transactionName != null) {
            byte[] bs = transactionName.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        GlobalBeginRequest globalBeginRequest = (GlobalBeginRequest) t;

        globalBeginRequest.setTimeout(in.getInt());
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            globalBeginRequest.setTransactionName(new String(bs, UTF8));
        }
    }

}
