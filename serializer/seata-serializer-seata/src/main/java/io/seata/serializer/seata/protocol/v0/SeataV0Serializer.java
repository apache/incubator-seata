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
package io.seata.serializer.seata.protocol.v0;

import com.sun.tools.javac.util.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.seata.common.loader.LoadLevel;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.serializer.seata.MessageSeataCodec;
import io.seata.serializer.seata.SeataAbstractSerializer;

import java.nio.ByteBuffer;

/**
 * The Seata codec v1.
 *
 * @author Bughue
 */
@LoadLevel(name = "SEATA", version = ProtocolConstants.VERSION_0)
public class SeataV0Serializer extends SeataAbstractSerializer {

    public SeataV0Serializer() {
        classMap.put(MessageType.TYPE_REG_CLT, new Pair<>(RegisterTMRequestCodec.class, RegisterTMRequest.class));
        classMap.put(MessageType.TYPE_REG_CLT_RESULT, new Pair<>(RegisterTMResponseCodec.class, RegisterTMResponse.class));
//        classMap.put(MessageType.TYPE_REG_RM, new Pair<>(RegisterRMRequestCodec.class, RegisterRMRequest.class));
//        classMap.put(MessageType.TYPE_REG_RM_RESULT, new Pair<>(RegisterRMResponseCodec.class, RegisterRMResponse.class));

    }

    @Override
    public <T> byte[] serialize(T t) {
        if (t == null || !(t instanceof AbstractMessage)) {
            throw new IllegalArgumentException("AbstractMessage isn't available.");
        }
        AbstractMessage abstractMessage = (AbstractMessage)t;
        //typecode
        short typecode = abstractMessage.getTypeCode();
        //msg codec
        MessageSeataCodec messageCodec = getCodecByType(typecode);
        //get empty ByteBuffer
        ByteBuf out = Unpooled.buffer(1024);
        //msg encode
        messageCodec.encode(t, out);
        byte[] body = new byte[out.readableBytes()];
        out.readBytes(body);

        //typecode + body
        ByteBuffer byteBuffer = ByteBuffer.allocate(body.length);
        byteBuffer.put(body);

        byteBuffer.flip();
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
        AbstractMessage abstractMessage = getMessageByType(typecode);
        //get messageCodec
        MessageSeataCodec messageCodec = getCodecByType(typecode);
        //decode
        messageCodec.decode(abstractMessage, in);
        return (T)abstractMessage;
    }


}
