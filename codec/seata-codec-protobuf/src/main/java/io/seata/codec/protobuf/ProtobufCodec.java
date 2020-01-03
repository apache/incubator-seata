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
import java.nio.charset.StandardCharsets;

import com.google.protobuf.GeneratedMessageV3;
import io.seata.codec.protobuf.convertor.PbConvertor;
import io.seata.codec.protobuf.manager.ProtobufConvertManager;
import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;

/**
 * The type Protobuf codec.
 *
 * @author leizhiyuan
 */
@LoadLevel(name = "PROTOBUF", order = 0)
public class ProtobufCodec implements Codec {

    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public <T> byte[] encode(T t) {
        if (t == null) {
            throw new NullPointerException();
        }

        //translate to pb
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
            t.getClass().getName());
        //for cross language,write FullName to data,which defines in proto file
        GeneratedMessageV3 newBody = (GeneratedMessageV3)pbConvertor.convert2Proto(t);
        byte[] body = ProtobufSerializer.serializeContent(newBody);
        final String name = newBody.getDescriptorForType().getFullName();
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
        final String descriptorName = new String(clazzName, UTF8);
        Class protobufClazz = ProtobufConvertManager.getInstance().fetchProtoClass(descriptorName);
        Object protobufObject = ProtobufSerializer.deserializeContent(protobufClazz.getName(), body);
        //translate back to core model
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(protobufClazz.getName());
        Object newBody = pbConvertor.convert2Model(protobufObject);
        return (T)newBody;
    }

}
