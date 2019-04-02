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

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.CodecHelper;
import com.alibaba.fescar.core.protocol.FragmentXID;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

/**
 * The type Abstract branch end response.
 *
 * @author sharajava
 */
public abstract class AbstractBranchEndResponse extends AbstractTransactionResponse {

    /**
     * The Branch status.
     */
    protected BranchStatus branchStatus;

    /**
     * The xid
     */
    protected FragmentXID xid;

    /**
     * The branch Id
     */
    protected long branchId;

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
     * Gets branch Id.
     *
     * @return the branch Id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch Id.
     *
     * @param branchId the branch Id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.put(xid.toBytes());
        CodecHelper.write(byteBuffer, branchId);
        byteBuffer.put((byte) branchStatus.getCode());
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        xid = FragmentXID.from(CodecHelper.readBytes(byteBuffer, FragmentXID.FIXED_BYTES));
        branchId = CodecHelper.readLong(byteBuffer);
        branchStatus = BranchStatus.get(byteBuffer.get());
    }

    @Override
    public boolean decode(ByteBuf in) {
        if (!super.decode(in)) {
            return false;
        }

        xid = FragmentXID.from(CodecHelper.readBytes(in, FragmentXID.FIXED_BYTES));
        branchId = CodecHelper.readLong(in);
        branchStatus = BranchStatus.get(in.readByte());
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
