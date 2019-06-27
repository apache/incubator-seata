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

import io.netty.buffer.ByteBuf;
import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.transaction.AbstractBranchEndRequest;
import io.seata.core.protocol.transaction.AbstractBranchEndResponse;

import java.nio.ByteBuffer;

/**
 * The type Abstract branch end response codec.
 *
 * @author zhangsen
 */
public abstract class AbstractBranchEndResponseCodec extends AbstractTransactionResponseCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractBranchEndResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out) {
        super.encode(t, out);

        AbstractBranchEndResponse abstractBranchEndResponse = (AbstractBranchEndResponse) t;
        String xid = abstractBranchEndResponse.getXid();
        long branchId = abstractBranchEndResponse.getBranchId();
        BranchStatus branchStatus = abstractBranchEndResponse.getBranchStatus();

        if (xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }
        out.putLong(branchId);
        out.put((byte)branchStatus.getCode());
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        super.decode(t, in);

        AbstractBranchEndResponse abstractBranchEndResponse = (AbstractBranchEndResponse) t;
        short xidLen = in.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            in.get(bs);
            abstractBranchEndResponse.setXid(new String(bs, UTF8));
        }
        abstractBranchEndResponse.setBranchId(in.getLong());
        abstractBranchEndResponse.setBranchStatus(BranchStatus.get(in.get()));
    }

}
