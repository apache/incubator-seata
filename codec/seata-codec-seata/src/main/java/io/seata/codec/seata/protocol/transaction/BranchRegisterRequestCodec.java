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

import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchRegisterRequest;

import java.nio.ByteBuffer;

/**
 * The type Branch register request codec.
 *
 * @author zhangsen
 */
public class BranchRegisterRequestCodec extends AbstractTransactionRequestToTCCodec {

    @Override
    public Class<?> getMessageClassType() {
        return BranchRegisterRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out) {
        BranchRegisterRequest branchRegisterRequest = (BranchRegisterRequest) t;

        String xid = branchRegisterRequest.getXid();
        BranchType branchType = branchRegisterRequest.getBranchType();
        String resourceId = branchRegisterRequest.getResourceId();
        String lockKey = branchRegisterRequest.getLockKey();
        String applicationData = branchRegisterRequest.getApplicationData();

        byte[] lockKeyBytes = null;
        if (lockKey != null) {
            lockKeyBytes = lockKey.getBytes(UTF8);
        }
        byte[] applicationDataBytes = null;
        if (applicationData != null) {
            applicationDataBytes = applicationData.getBytes(UTF8);
        }

        // 1. xid
        if (xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }
        // 2. Branch Type
        out.put((byte)branchType.ordinal());

        // 3. Resource Id
        if (resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }

        // 4. Lock Key
        if (lockKey != null) {
            out.putInt(lockKeyBytes.length);
            if (lockKeyBytes.length > 0) {
                out.put(lockKeyBytes);
            }
        } else {
            out.putInt(0);
        }

        //5. applicationData
        if (applicationData != null) {
            out.putInt(applicationDataBytes.length);
            if (applicationDataBytes.length > 0) {
                out.put(applicationDataBytes);
            }
        } else {
            out.putInt(0);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        BranchRegisterRequest branchRegisterRequest = (BranchRegisterRequest) t;

        short xidLen = in.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            in.get(bs);
            branchRegisterRequest.setXid(new String(bs, UTF8));
        }
        branchRegisterRequest.setBranchType(BranchType.get(in.get()));
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            branchRegisterRequest.setResourceId(new String(bs, UTF8));
        }

        int iLen = in.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            in.get(bs);
            branchRegisterRequest.setLockKey(new String(bs, UTF8));
        }

        int applicationDataLen = in.getInt();
        if (applicationDataLen > 0) {
            byte[] bs = new byte[applicationDataLen];
            in.get(bs);
            branchRegisterRequest.setApplicationData(new String(bs, UTF8));
        }
    }

}
