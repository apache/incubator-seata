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
package io.seata.serializer.seata.protocol.transaction;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;

/**
 * The type Global lock query response codec.
 *
 * @author zhangsen
 */
public class GlobalLockQueryResponseCodec extends AbstractTransactionResponseCodec {

    @Override
    public Class<?> getMessageClassType() {
        return GlobalLockQueryResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);

        GlobalLockQueryResponse globalLockQueryResponse = (GlobalLockQueryResponse)t;
        boolean lockable = globalLockQueryResponse.isLockable();
        out.writeShort((short)(lockable ? 1 : 0));
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        super.decode(t, in);

        GlobalLockQueryResponse globalLockQueryResponse = (GlobalLockQueryResponse)t;
        globalLockQueryResponse.setLockable(in.getShort() == 1);
    }
}
