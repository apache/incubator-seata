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

import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchReportRequest;

import java.nio.ByteBuffer;

/**
 * The type Branch report request codec.
 *
 * @author zhangsen
 */
public class BranchReportRequestCodec extends AbstractTransactionRequestToTCCodec {

    @Override
    public Class<?> getMessageClassType() {
        return BranchReportRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuffer out) {
        BranchReportRequest branchReportRequest = (BranchReportRequest) t;
        String xid = branchReportRequest.getXid();
        long branchId = branchReportRequest.getBranchId();
        BranchStatus status = branchReportRequest.getStatus();
        String resourceId = branchReportRequest.getResourceId();
        String applicationData = branchReportRequest.getApplicationData();
        BranchType branchType = branchReportRequest.getBranchType();

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
        // 2. Branch Id
        out.putLong(branchId);
        // 3. Branch Status
        out.put((byte)status.getCode());
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
        if (applicationData != null) {
            out.putInt(applicationDataBytes.length);
            if (applicationDataBytes.length > 0) {
                out.put(applicationDataBytes);
            }
        } else {
            out.putInt(0);
        }
        //6. branchType
        out.put((byte)branchType.ordinal());
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        BranchReportRequest branchReportRequest = (BranchReportRequest) t;

        short xidLen = in.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            in.get(bs);
            branchReportRequest.setXid(new String(bs, UTF8));
        }
        branchReportRequest.setBranchId(in.getLong());
        branchReportRequest.setStatus(BranchStatus.get(in.get()));
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            branchReportRequest.setResourceId(new String(bs, UTF8));
        }

        int iLen = in.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            in.get(bs);
            branchReportRequest.setApplicationData(new String(bs, UTF8));
        }
        branchReportRequest.setBranchType(BranchType.get(in.get()));
    }


}
