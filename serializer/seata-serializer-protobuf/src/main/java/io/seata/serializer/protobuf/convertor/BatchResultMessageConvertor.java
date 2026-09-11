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
package io.seata.serializer.protobuf.convertor;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.serializer.protobuf.generated.AbstractMessageProto;
import io.seata.serializer.protobuf.generated.BatchResultMessageProto;
import io.seata.serializer.protobuf.generated.MessageTypeProto;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;

/**
 * The type batch result message protobuf convertor.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.5.0
 */
public class BatchResultMessageConvertor implements PbConvertor<BatchResultMessage, BatchResultMessageProto> {

    @Override
    public BatchResultMessageProto convert2Proto(BatchResultMessage batchResultMessage) {

        final short typeCode = batchResultMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        List<Any> lists = new ArrayList<>();
        batchResultMessage.getResultMessages().forEach(msg -> {
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().
                fetchConvertor(msg.getClass().getName());
            lists.add(Any.pack((Message) pbConvertor.convert2Proto(msg)));
        });

        return BatchResultMessageProto.newBuilder()
            .setAbstractMessage(abstractMessage)
            .addAllResultMessages(lists)
            .addAllMsgIds(batchResultMessage.getMsgIds())
            .build();

    }

    @Override
    public BatchResultMessage convert2Model(BatchResultMessageProto batchResultMessageProto) {
        BatchResultMessage result = new BatchResultMessage();
        List<Any> anys = batchResultMessageProto.getResultMessagesList();
        anys.forEach(any -> {
            final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(
                getTypeNameFromTypeUrl(any.getTypeUrl()));
            if (any.is(clazz)) {
                try {
                    Object ob = any.unpack(clazz);
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(
                        clazz.getName());
                    Object model = pbConvertor.convert2Model(ob);
                    result.getResultMessages().add((AbstractResultMessage) model);
                } catch (InvalidProtocolBufferException e) {
                    throw new ShouldNeverHappenException(e);
                }
            }
        });
        result.setMsgIds(batchResultMessageProto.getMsgIdsList());
        return result;
    }

    private static String getTypeNameFromTypeUrl(String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        return pos == -1 ? "" : typeUrl.substring(pos + 1);
    }
}