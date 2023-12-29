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
package io.seata.serializer.seata.protocol.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.seata.common.util.BufferUtils;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.serializer.Serializer;
import io.seata.serializer.seata.MessageCodecFactory;
import io.seata.serializer.seata.MessageSeataCodec;
import io.seata.serializer.seata.protocol.v0.SeataSerializerV0;

import java.nio.ByteBuffer;

/**
 * The Seata codec v1.
 */
public class SeataSerializerV1 implements Serializer {


    static Serializer instance = new SeataSerializerV1();

    public static Serializer getInstance() {
        return instance;
    }

    private SeataSerializerV1() {
    }

    @Override
    public <T> byte[] serialize(T t) {
        if (!(t instanceof AbstractMessage)) {
            throw new IllegalArgumentException("AbstractMessage isn't available.");
        }
        AbstractMessage abstractMessage = (AbstractMessage) t;
        //type code
        short typecode = abstractMessage.getTypeCode();
        //msg codec
        MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typecode, ProtocolConstants.VERSION_1);
        //get empty ByteBuffer
        ByteBuf out = Unpooled.buffer(1024);
        //msg encode
        messageCodec.encode(t, out);
        byte[] body = new byte[out.readableBytes()];
        out.readBytes(body);

        ByteBuffer byteBuffer;

        //typecode + body
        byteBuffer = ByteBuffer.allocate(2 + body.length);
        byteBuffer.putShort(typecode);
        byteBuffer.put(body);

        BufferUtils.flip(byteBuffer);
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Nothing to decode.");
        }
        if (bytes.length < 2) {
            throw new IllegalArgumentException("The byte[] isn't available for decode.");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        //typecode
        short typecode = byteBuffer.getShort();
        //msg body
        byte[] body = new byte[byteBuffer.remaining()];
        byteBuffer.get(body);
        ByteBuffer in = ByteBuffer.wrap(body);
        //new Messgae
        AbstractMessage abstractMessage = MessageCodecFactory.getMessage(typecode);
        //get messageCodec
        MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typecode, ProtocolConstants.VERSION_1);
        //decode
        messageCodec.decode(abstractMessage, in);
        return (T) abstractMessage;
    }

}
