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

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.CodecHelper;
import com.alibaba.fescar.core.protocol.FragmentXID;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

/**
 * The type Abstract branch end request.
 */
public abstract class AbstractBranchEndRequest extends AbstractTransactionRequestToRM {

    private static final long serialVersionUID = 5083828939317068713L;

    /**
     * The Xid.
     */
    protected FragmentXID xid;

    /**
     * The Branch id.
     */
    protected long branchId;

    /**
     * The Branch type.
     */
    protected BranchType branchType = BranchType.AT;

    /**
     * The Resource id.
     */
    protected String resourceId;

    /**
     * The Application data.
     */
    protected String applicationData;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public FragmentXID getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(FragmentXID xid) {
        this.xid = xid;
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
    public byte[] encode() {
        byte[] applicationDataBytes = null;
        if (this.applicationData != null) {
            applicationDataBytes = applicationData.getBytes(UTF8);
            if (applicationDataBytes.length > 512) {
                byteBuffer = ByteBuffer.allocate(applicationDataBytes.length + 1024);
            }
        }

        // 1. xid
        byteBuffer.put(xid.toBytes());

        // 2. Branch Id
        byteBuffer.putLong(this.branchId);
        // 3. Branch Type
        byteBuffer.put((byte) this.branchType.ordinal());
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
    public boolean decode(ByteBuf in) {
        xid = FragmentXID.from(CodecHelper.readBytes(in, FragmentXID.FIXED_BYTES));
        branchId = CodecHelper.readLong(in);
        branchType = BranchType.get(in.readByte());
        resourceId = CodecHelper.readString(in);
        applicationData = CodecHelper.readBigString(in);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("xid=<");
        result.append(xid);
        result.append(">,");
        result.append("branchId=");
        result.append(branchId);
        result.append(",");
        result.append("branchType=");
        result.append(branchType);
        result.append(",");
        result.append("resourceId=");
        result.append(resourceId);
        result.append(",");
        result.append("applicationData=");
        result.append(applicationData);

        return result.toString();
    }
}
