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

import com.alibaba.fescar.core.protocol.MergedMessage;
import com.alibaba.fescar.core.rpc.RpcContext;

/**
 * The type Global begin request.
 */
public class GlobalBeginRequest extends AbstractTransactionRequestToTC implements MergedMessage {

    private static final long serialVersionUID = 7236162274218388376L;

    private int timeout = 60000;

    private String transactionName;

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Sets transaction name.
     *
     * @param transactionName the transaction name
     */
    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    @Override
    public short getTypeCode() {
        return TYPE_GLOBAL_BEGIN;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        byteBuffer.putInt(timeout);

        if (this.transactionName != null) {
            byte[] bs = transactionName.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.timeout = byteBuffer.getInt();

        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.setTransactionName(new String(bs, UTF8));
        }
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this, rpcContext);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("timeout=");
        result.append(timeout);
        result.append(",");
        result.append("transactionName=");
        result.append(transactionName);

        return result.toString();
    }
}
