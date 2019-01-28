/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol.transaction;

import java.nio.ByteBuffer;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.MergedMessage;
import com.alibaba.fescar.core.rpc.RpcContext;

/**
 * The type Branch report request.
 */
public class BranchReportRequest extends AbstractTransactionRequestToTC implements MergedMessage {

    private long transactionId;

    private long branchId;

    private String resourceId;

    private BranchStatus status;

    private String applicationData;

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public BranchStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(BranchStatus status) {
        this.status = status;
    }

    /**
     * Gets application data.
     *
     * @return the application data
     */
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    @Override
    public short getTypeCode() {
        return TYPE_BRANCH_STATUS_REPORT;
    }

    @Override
    public byte[] encode() {
        byte[] applicationDataBytes = null;
        if (this.applicationData != null) {
            applicationDataBytes = applicationData.getBytes(UTF8);
            if (applicationDataBytes.length > 512) {
                byteBuffer = ByteBuffer.allocate(applicationDataBytes.length + 1024);
            }
        }

        // 1. Transaction Id
        byteBuffer.putLong(this.transactionId);
        // 2. Branch Id
        byteBuffer.putLong(this.branchId);
        // 3. Branch Status
        byteBuffer.put((byte) this.status.ordinal());
        // 4. Resource Id
        if (this.resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            byteBuffer.putShort((short) bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short) 0);
        }

        // 5. Application Data
        if (this.applicationData != null) {
            byteBuffer.putInt(applicationDataBytes.length);
            if (applicationDataBytes.length > 0) {
                byteBuffer.put(applicationDataBytes);
            }
        } else {
            byteBuffer.putInt(0);
        }

        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.transactionId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
        this.status = BranchStatus.get(byteBuffer.get());
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.resourceId = new String(bs, UTF8);
        }

        int iLen = byteBuffer.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            byteBuffer.get(bs);
            this.applicationData = new String(bs, UTF8);
        }
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this, rpcContext);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("transactionId=");
        result.append(transactionId);
        result.append(",");
        result.append("branchId=");
        result.append(branchId);
        result.append(",");
        result.append("resourceId=");
        result.append(resourceId);
        result.append(",");
        result.append("status=");
        result.append(status);
        result.append(",");
        result.append("applicationData=");
        result.append(applicationData);

        return result.toString();
    }
}
