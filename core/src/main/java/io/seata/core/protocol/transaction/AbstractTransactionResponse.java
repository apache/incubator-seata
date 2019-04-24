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

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.AbstractResultMessage;

import io.netty.buffer.ByteBuf;
import io.seata.core.exception.TransactionExceptionCode;

/**
 * The type Abstract transaction response.
 *
 * @author sharajava
 */
public abstract class AbstractTransactionResponse extends AbstractResultMessage {

    private TransactionExceptionCode transactionExceptionCode = TransactionExceptionCode.Unknown;

    /**
     * Gets transaction exception code.
     *
     * @return the transaction exception code
     */
    public TransactionExceptionCode getTransactionExceptionCode() {
        return transactionExceptionCode;
    }

    /**
     * Sets transaction exception code.
     *
     * @param transactionExceptionCode the transaction exception code
     */
    public void setTransactionExceptionCode(TransactionExceptionCode transactionExceptionCode) {
        this.transactionExceptionCode = transactionExceptionCode;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.put((byte)transactionExceptionCode.ordinal());
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        transactionExceptionCode = TransactionExceptionCode.get(byteBuffer.get());
    }

    @Override
    public boolean decode(ByteBuf in) {
        boolean s = super.decode(in);
        if (!s) {
            return s;
        }
        transactionExceptionCode = TransactionExceptionCode.get(in.readByte());
        return true;
    }
}
