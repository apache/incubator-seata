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
package io.seata.core.protocol.transaction;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.seata.core.model.BranchStatus;

/**
 * The type Abstract branch end response.
 *
 * @author sharajava
 */
public abstract class AbstractBranchEndResponse extends AbstractTransactionResponse {

    /**
     * The Xid.
     */
    protected String xid;

    /**
     * The Branch id.
     */
    protected long branchId;
    /**
     * The Branch status.
     */
    protected BranchStatus branchStatus;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
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
     * Gets branch status.
     *
     * @return the branch status
     */
    public BranchStatus getBranchStatus() {
        return branchStatus;
    }

    /**
     * Sets branch status.
     *
     * @param branchStatus the branch status
     */
    public void setBranchStatus(BranchStatus branchStatus) {
        this.branchStatus = branchStatus;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        if (this.xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }
        byteBuffer.putLong(this.branchId);
        byteBuffer.put((byte)branchStatus.getCode());
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        short xidLen = byteBuffer.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            byteBuffer.get(bs);
            this.setXid(new String(bs, UTF8));
        }
        branchId = byteBuffer.getLong();
        branchStatus = BranchStatus.get(byteBuffer.get());
    }

    @Override
    public boolean decode(ByteBuf in) {
        boolean s = super.decode(in);
        if (!s) {
            return s;
        }
        short xidLen;
        if (in.readableBytes() < 2) {
            return false;
        }
        xidLen = in.readShort();
        if (in.readableBytes() < xidLen) {
            return false;
        }
        byte[] bs = new byte[xidLen];
        in.readBytes(bs);
        this.setXid(new String(bs, UTF8));
        if (in.readableBytes() < 8) {
            return false;
        }
        branchId = in.readLong();
        if (in.readableBytes() < 1) {
            return false;
        }
        branchStatus = BranchStatus.get(in.readByte());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("xid=");
        result.append(xid);
        result.append(",");
        result.append("branchId=");
        result.append(branchId);
        result.append(",");
        result.append("branchStatus=");
        result.append(branchStatus);
        result.append(",");
        result.append("result code =");
        result.append(getResultCode());
        result.append(",");
        result.append("getMsg =");
        result.append(getMsg());

        return result.toString();
    }
}
