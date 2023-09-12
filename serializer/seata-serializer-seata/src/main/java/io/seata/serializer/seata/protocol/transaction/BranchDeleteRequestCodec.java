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

import io.netty.buffer.ByteBuf;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchDeleteRequest;

import java.nio.ByteBuffer;

public class BranchDeleteRequestCodec extends AbstractTransactionRequestToRMCodec {
    @Override
    public <T> void encode(T t, ByteBuf out) {
        BranchDeleteRequest branchDeleteRequest = (BranchDeleteRequest) t;
        String xid = branchDeleteRequest.getXid();
        long branchId = branchDeleteRequest.getBranchId();
        BranchType branchType = branchDeleteRequest.getBranchType();
        String resourceId = branchDeleteRequest.getResourceId();

        // 1. xid
        if (xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            out.writeShort((short) bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short) 0);
        }
        // 2. Branch Id
        out.writeLong(branchId);
        // 3. Branch Type
        out.writeByte(branchType.ordinal());
        // 4. Resource Id
        if (resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
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
        BranchDeleteRequest branchDeleteRequest = (BranchDeleteRequest) t;

        int xidLen = 0;
        if (in.remaining() >= 2) {
            xidLen = in.getShort();
        }
        if (xidLen <= 0) {
            return;
        }
        if (in.remaining() < xidLen) {
            return;
        }
        byte[] bs = new byte[xidLen];
        in.get(bs);
        branchDeleteRequest.setXid(new String(bs, UTF8));

        if (in.remaining() < 8) {
            return;
        }
        branchDeleteRequest.setBranchId(in.getLong());

        if (in.remaining() < 1) {
            return;
        }
        branchDeleteRequest.setBranchType(BranchType.get(in.get()));

        int resourceIdLen = 0;
        if (in.remaining() < 2) {
            return;
        }
        resourceIdLen = in.getShort();

        if (resourceIdLen <= 0) {
            return;
        }
        if (in.remaining() < resourceIdLen) {
            return;
        }
        bs = new byte[resourceIdLen];
        in.get(bs);
        branchDeleteRequest.setResourceId(new String(bs, UTF8));
    }
}
