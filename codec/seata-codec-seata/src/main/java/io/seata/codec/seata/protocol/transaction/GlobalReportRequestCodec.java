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
package io.seata.codec.seata.protocol.transaction;

import io.netty.buffer.ByteBuf;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.GlobalReportRequest;

import java.nio.ByteBuffer;

/**
 * The type Global status report codec.
 *
 * @author lorne.cl
 */
public class GlobalReportRequestCodec extends AbstractGlobalEndRequestCodec {

    @Override
    public Class<?> getMessageClassType() {
        return GlobalReportRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);

        GlobalReportRequest reportRequest = (GlobalReportRequest)t;
        GlobalStatus globalStatus = reportRequest.getGlobalStatus();
        if (globalStatus != null) {
            out.writeByte((byte)globalStatus.getCode());
        } else {
            out.writeByte((byte)-1);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        super.decode(t, in);

        GlobalReportRequest reportRequest = (GlobalReportRequest)t;
        byte b = in.get();
        if (b > -1) {
            reportRequest.setGlobalStatus(GlobalStatus.get(b));
        }
    }
}
