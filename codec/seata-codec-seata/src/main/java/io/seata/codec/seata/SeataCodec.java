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
package io.seata.codec.seata;

import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;
import io.seata.core.protocol.AbstractMessage;

import java.nio.ByteBuffer;

/**
 * The Seata codec.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
@LoadLevel(name="SEATA")
public class SeataCodec implements Codec {

    @Override
    public <T> byte[] encode(T t) {
        if(t == null || !(t instanceof AbstractMessage)){
            throw new IllegalArgumentException("AbstractMessage isn't available.");
        }
        AbstractMessage abstractMessage = (AbstractMessage) t;
        //typecode
        short typecode = abstractMessage.getTypeCode();
        //msg codec
        MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typecode);
        //get empty ByteBuffer
        ByteBuffer out = MessageCodecFactory.getByteBuffer(abstractMessage);
        //msg encode
        messageCodec.encode(t, out);
        out.flip();
        byte[] body = new byte[out.limit()];
        out.get(body);

        //typecode + body
        ByteBuffer byteBuffer = ByteBuffer.allocate(4+ body.length);
        byteBuffer.putShort(typecode);
        byteBuffer.put(body);

        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public <T> T decode(byte[] bytes) {
        if(bytes == null || bytes.length == 0){
            throw new IllegalArgumentException("Nothing to decode.");
        }
        if(bytes.length < 2){
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
        MessageSeataCodec messageCodec = MessageCodecFactory.getMessageCodec(typecode);
        //decode
        messageCodec.decode(abstractMessage, in);
        return (T) abstractMessage;
    }


}
