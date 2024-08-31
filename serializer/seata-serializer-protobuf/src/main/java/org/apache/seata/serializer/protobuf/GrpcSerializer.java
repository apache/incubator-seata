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
package org.apache.seata.serializer.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.serializer.protobuf.convertor.PbConvertor;
import org.apache.seata.serializer.protobuf.manager.ProtobufConvertManager;

@LoadLevel(name = "GRPC")
public class GrpcSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T t) {
        PbConvertor pbConvertor = ProtobufConvertManager.getInstance()
                .fetchConvertor(t.getClass().getName());
        Any grpcBody = Any.pack((Message) pbConvertor.convert2Proto(t));

        return grpcBody.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        try {
            Any body = Any.parseFrom(bytes);
            final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(getTypeNameFromTypeUrl(body.getTypeUrl()));
            if (body.is(clazz)) {
                Object ob = body.unpack(clazz);
                PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(clazz.getName());

                return (T) pbConvertor.convert2Model(ob);
            }
        } catch (Throwable e) {
            throw new ShouldNeverHappenException("GrpcSerializer deserialize error", e);
        }

        return null;
    }

    private String getTypeNameFromTypeUrl(String typeUri) {
        int pos = typeUri.lastIndexOf('/');
        return pos == -1 ? "" : typeUri.substring(pos + 1);
    }
}
