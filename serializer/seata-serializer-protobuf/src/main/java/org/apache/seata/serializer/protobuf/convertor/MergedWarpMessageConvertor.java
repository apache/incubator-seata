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
package org.apache.seata.serializer.protobuf.convertor;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.MergedWarpMessageProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.serializer.protobuf.manager.ProtobufConvertManager;


public class MergedWarpMessageConvertor implements PbConvertor<MergedWarpMessage, MergedWarpMessageProto> {

    @Override
    public MergedWarpMessageProto convert2Proto(MergedWarpMessage mergedWarpMessage) {

        final short typeCode = mergedWarpMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        List<Any> lists = new ArrayList<>();
        for (AbstractMessage msg : mergedWarpMessage.msgs) {
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
                msg.getClass().getName());
            lists.add(Any.pack((Message)pbConvertor.convert2Proto(msg)));
        }

        MergedWarpMessageProto mergedWarpMessageProto = MergedWarpMessageProto.newBuilder().setAbstractMessage(
            abstractMessage).addAllMsgs(lists).addAllMsgIds(mergedWarpMessage.msgIds).build();

        return mergedWarpMessageProto;

    }

    @Override
    public MergedWarpMessage convert2Model(MergedWarpMessageProto mergedWarpMessageProto) {
        MergedWarpMessage result = new MergedWarpMessage();
        List<Any> anys = mergedWarpMessageProto.getMsgsList();
        for (Any any : anys) {
            final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(
                getTypeNameFromTypeUrl(any.getTypeUrl()));
            if (any.is(clazz)) {
                try {
                    Object ob = any.unpack(clazz);
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(
                        clazz.getName());
                    Object model = pbConvertor.convert2Model(ob);
                    result.msgs.add((AbstractMessage)model);
                } catch (InvalidProtocolBufferException e) {
                    throw new ShouldNeverHappenException(e);
                }
            }
        }
        result.msgIds = mergedWarpMessageProto.getMsgIdsList();

        return result;
    }

    private static String getTypeNameFromTypeUrl(java.lang.String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        return pos == -1 ? "" : typeUrl.substring(pos + 1);
    }
}
