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

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.MergedMessage;
import com.alibaba.fescar.core.rpc.RpcContext;

/**
 * The type Branch register request.
 */
public class BranchRegisterRequest extends AbstractTransactionRequestToTC implements MergedMessage {

    private static final long serialVersionUID = 1242711598812634704L;

    private long transactionId;

    private BranchType branchType = BranchType.AT;

    private String resourceId;

    private String lockKey;

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
     * Gets branch type.
     *
     * @return the branch type
     */
    public BranchType getBranchType() {
        return branchType;
    }

    /**
     * Sets branch type.
     *
     * @param branchType the branch type
     */
    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    /**
     * Gets lock key.
     *
     * @return the lock key
     */
    public String getLockKey() {
        return lockKey;
    }

    /**
     * Sets lock key.
     *
     * @param lockKey the lock key
     */
    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
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

    @Override
    public short getTypeCode() {
        return TYPE_BRANCH_REGISTER;
    }

    public String getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    @Override
    public byte[] encode() {
        int byteLenth = 0;
        byte[] lockKeyBytes = null;
        if (this.lockKey != null) {
            lockKeyBytes = lockKey.getBytes(UTF8);
            if (lockKeyBytes.length > 512) {
                byteLenth += lockKeyBytes.length;
            }
        }
        byte[] applicationDataBytes = null;
        if (this.applicationData != null) {
            applicationDataBytes = applicationData.getBytes(UTF8);
            if (applicationDataBytes.length > 512) {
                byteLenth += applicationDataBytes.length;
            }
        }
        byteBuffer = ByteBuffer.allocate(byteLenth + 1024);

        // 1. Transaction Id
        byteBuffer.putLong(this.transactionId);
        // 2. Branch Type
        byteBuffer.put((byte)this.branchType.ordinal());
        // 3. Resource Id
        if (this.resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        // 4. Lock Key
        if (this.lockKey != null) {
            byteBuffer.putInt(lockKeyBytes.length);
            if (lockKeyBytes.length > 0) {
                byteBuffer.put(lockKeyBytes);
            }
        } else {
            byteBuffer.putInt(0);
        }

        //5. applicationData
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
        this.branchType = BranchType.get(byteBuffer.get());
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.setResourceId(new String(bs, UTF8));
        }

        int iLen = byteBuffer.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            byteBuffer.get(bs);
            this.setLockKey(new String(bs, UTF8));
        }

        int applicationDataLen = byteBuffer.getInt();
        if (applicationDataLen > 0) {
            byte[] bs = new byte[applicationDataLen];
            byteBuffer.get(bs);
            setApplicationData(new String(bs, UTF8));
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
        result.append("branchType=");
        result.append(branchType);
        result.append(",");
        result.append("resourceId=");
        result.append(resourceId);
        result.append(",");
        result.append("lockKey=");
        result.append(lockKey);

        return result.toString();
    }
}
