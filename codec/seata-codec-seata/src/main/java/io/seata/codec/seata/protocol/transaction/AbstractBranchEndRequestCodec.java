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
import io.seata.core.protocol.transaction.AbstractBranchEndRequest;

import java.nio.ByteBuffer;

/**
 * The type Abstract branch end request codec.
 *
 * @author zhangsen
 */
public abstract class AbstractBranchEndRequestCodec extends AbstractTransactionRequestToRMCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractBranchEndRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out){
        AbstractBranchEndRequest abstractBranchEndRequest = (AbstractBranchEndRequest) t;
        String  xid = abstractBranchEndRequest.getXid();
        long branchId =  abstractBranchEndRequest.getBranchId();
        BranchType branchType = abstractBranchEndRequest.getBranchType();
        String resourceId = abstractBranchEndRequest.getResourceId();
        String applicationData = abstractBranchEndRequest.getApplicationData();

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
        // 2. Branch Id
        out.putLong(branchId);
        // 3. Branch Type
        out.put((byte)branchType.ordinal());
        // 4. Resource Id
        if (resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            out.putShort((short)bs.length);
            if (bs.length > 0) {
                out.put(bs);
            }
        } else {
            out.putShort((short)0);
        }

        // 5. Application Data
        byte[] applicationDataBytes = null;
        if (applicationData != null) {
            applicationDataBytes = applicationData.getBytes(UTF8);
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
        AbstractBranchEndRequest abstractBranchEndRequest = (AbstractBranchEndRequest) t;

        int xidLen = 0;
        if (in.remaining() >= 2) {
            xidLen = in.getShort();
        }
        if (xidLen <= 0) {
            return ;
        }
        if (in.remaining() < xidLen) {
            return ;
        }
        byte[] bs = new byte[xidLen];
        in.get(bs);
        abstractBranchEndRequest.setXid(new String(bs, UTF8));

        if (in.remaining() < 8) {
            return ;
        }
        abstractBranchEndRequest.setBranchId(in.getLong());

        if (in.remaining() < 1) {
            return ;
        }
        abstractBranchEndRequest.setBranchType(BranchType.get(in.get()));

        int resourceIdLen = 0;
        if (in.remaining() < 2) {
            return ;
        }
        resourceIdLen = in.getShort();

        if (resourceIdLen <= 0) {
            return ;
        }
        if (in.remaining() < resourceIdLen) {
            return ;
        }
        bs = new byte[resourceIdLen];
        in.get(bs);
        abstractBranchEndRequest.setResourceId(new String(bs, UTF8));

        int applicationDataLen = 0;
        if (in.remaining() < 4) {
            return ;
        }
        applicationDataLen = in.getInt();

        if (applicationDataLen > 0) {
            if (in.remaining() < applicationDataLen) {
                return ;
            }
            bs = new byte[applicationDataLen];
            in.get(bs);
            abstractBranchEndRequest.setApplicationData(new String(bs, UTF8));
        }
    }


}
