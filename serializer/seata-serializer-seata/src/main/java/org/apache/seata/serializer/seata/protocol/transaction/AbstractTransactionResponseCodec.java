/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.seata.protocol.transaction;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.apache.seata.serializer.seata.protocol.AbstractResultMessageCodec;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;

/**
 * The type Abstract transaction response codec.
 *
 */
public abstract class AbstractTransactionResponseCodec extends AbstractResultMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractTransactionResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);

        AbstractTransactionResponse abstractTransactionResponse = (AbstractTransactionResponse)t;
        TransactionExceptionCode transactionExceptionCode = abstractTransactionResponse.getTransactionExceptionCode();
        out.writeByte(transactionExceptionCode.ordinal());
    }

    @Override
    public <T> void decode(T t, ByteBuffer out) {
        super.decode(t, out);

        AbstractTransactionResponse abstractTransactionResponse = (AbstractTransactionResponse)t;
        abstractTransactionResponse.setTransactionExceptionCode(TransactionExceptionCode.get(out.get()));
    }

}
