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

import com.alibaba.fescar.core.protocol.CodecHelper;
import com.alibaba.fescar.core.protocol.MergedMessage;
import java.nio.ByteBuffer;

/**
 * The type Abstract global end request.
 *
 * @author sharajava
 */
public abstract class AbstractGlobalEndRequest extends AbstractTransactionRequestToTC implements MergedMessage {

    /**
     * The Transaction id.
     */
    protected long transactionId;

    /**
     * The Fragment id
     */
    private long fragmentId;

    /**
     * The Extra data.
     */
    protected String extraData;

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
     * Gets fragment id.
     *
     * @return the fragment id
     */
    public long getFragmentId() {
        return fragmentId;
    }

    /**
     * Sets fragment id.
     *
     * @param fragmentId the fragment id
     */
    public void setFragmentId(long fragmentId) {
        this.fragmentId = fragmentId;
    }

    /**
     * Gets extra data.
     *
     * @return the extra data
     */
    public String getExtraData() {
        return extraData;
    }

    /**
     * Sets extra data.
     *
     * @param extraData the extra data
     */
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        byteBuffer.putLong(this.transactionId);
        byteBuffer.putLong(this.fragmentId);
        if (this.extraData != null) {
            byte[] bs = extraData.getBytes(UTF8);
            byteBuffer.putShort((short) bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short) 0);
        }

        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        transactionId = CodecHelper.readLong(byteBuffer);
        fragmentId = CodecHelper.readLong(byteBuffer);
        extraData = CodecHelper.readString(byteBuffer);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("transactionId=");
        result.append(transactionId);
        result.append(",");
        result.append("fragmentId=");
        result.append(fragmentId);
        result.append(",");
        result.append("extraData=");
        result.append(extraData);

        return result.toString();
    }
}
