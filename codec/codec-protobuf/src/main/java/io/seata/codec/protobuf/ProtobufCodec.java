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
package io.seata.codec.protobuf;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.serialize.ProtobufConvertManager;

/**
 * The type Protobuf codec.
 *
 * @author leizhiyuan
 * @data 2019 /5/6
 */
@LoadLevel(name = "protobuf", order = 0)
public class ProtobufCodec implements Codec {

    protected static final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public <T> byte[] encode(T t) {
        if (t == null) {
            throw new NullPointerException();
        }

        //translate to pb
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
            t.getClass().getName());
        Object newBody = pbConvertor.convert2Proto(t);

        byte[] body = ProtobufSerialzer.serializeContent(newBody);
        final String name = t.getClass().getName();
        final byte[] nameBytes = name.getBytes(UTF8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + nameBytes.length + body.length);
        byteBuffer.putInt(nameBytes.length);
        byteBuffer.put(nameBytes);
        byteBuffer.put(body);
        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public <T> T decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException();
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        int clazzNameLength = byteBuffer.getInt();
        byte[] clazzName = new byte[clazzNameLength];
        byteBuffer.get(clazzName);
        byte[] body = new byte[bytes.length - clazzNameLength - 4];
        byteBuffer.get(body);
        final String clazz = new String(clazzName, UTF8);
        Object protobufObject = ProtobufSerialzer.deserializeContent(clazz, bytes);
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(clazz);
        Object newBody = pbConvertor.convert2Model(protobufObject);
        return (T)newBody;
    }

}
