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
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.transaction.AbstractGlobalEndResponse;

/**
 * The type Abstract global end response codec.
 *
 */
public abstract class AbstractGlobalEndResponseCodec extends AbstractTransactionResponseCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractGlobalEndResponse.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf in) {
        super.encode(t, in);

        AbstractGlobalEndResponse abstractGlobalEndResponse = (AbstractGlobalEndResponse)t;
        GlobalStatus globalStatus = abstractGlobalEndResponse.getGlobalStatus();
        in.writeByte(globalStatus.getCode());
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        super.decode(t, in);

        AbstractGlobalEndResponse abstractGlobalEndResponse = (AbstractGlobalEndResponse)t;

        abstractGlobalEndResponse.setGlobalStatus(GlobalStatus.get(in.get()));
    }

}
