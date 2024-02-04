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
package org.apache.seata.serializer.seata.protocol;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.ResultCode;

/**
 * The type Abstract result message codec.
 */
public abstract class AbstractResultMessageCodec extends AbstractMessageCodec {

    @Override
    public Class<?> getMessageClassType() {
        return AbstractResultMessage.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        AbstractResultMessage abstractResultMessage = (AbstractResultMessage) t;
        ResultCode resultCode = abstractResultMessage.getResultCode();
        String resultMsg = abstractResultMessage.getMsg();
        if (null != resultCode) {
            out.writeByte(resultCode.ordinal());
        } else{
            out.writeByte(ResultCode.values().length);
        }
        if (resultCode != ResultCode.Success) {
            if (StringUtils.isNotEmpty(resultMsg)) {
                String msg;
                if (resultMsg.length() > Short.MAX_VALUE) {
                    msg = resultMsg.substring(0, Short.MAX_VALUE);
                } else {
                    msg = resultMsg;
                }
                byte[] bs = msg.getBytes(UTF8);
                out.writeShort((short) bs.length);
                out.writeBytes(bs);
            } else {
                out.writeShort((short) 0);
            }
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        AbstractResultMessage abstractResultMessage = (AbstractResultMessage) t;
        ResultCode resultCode = null;
        if(in.get()<ResultCode.values().length){
            resultCode = ResultCode.get(in.get());
            abstractResultMessage.setResultCode(resultCode);
        }
        if (resultCode != ResultCode.Success) {
            short len = in.getShort();
            if (len > 0) {
                byte[] msg = new byte[len];
                in.get(msg);
                abstractResultMessage.setMsg(new String(msg, UTF8));
            }
        }
    }

}
