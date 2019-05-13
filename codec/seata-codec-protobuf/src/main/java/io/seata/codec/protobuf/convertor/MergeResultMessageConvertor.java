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
package io.seata.codec.protobuf.convertor;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.MergedResultMessageProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.manager.ProtobufConvertManager;

/**
 * @author leizhiyuan
 */
public class MergeResultMessageConvertor implements PbConvertor<MergeResultMessage, MergedResultMessageProto> {
    @Override
    public MergedResultMessageProto convert2Proto(MergeResultMessage mergeResultMessage) {
        final short typeCode = mergeResultMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        List<Any> lists = new ArrayList<>();
        for (AbstractMessage msg : mergeResultMessage.msgs) {
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
                msg.getClass().getName());
            lists.add(Any.pack((Message)pbConvertor.convert2Proto(msg)));
        }

        MergedResultMessageProto mergedWarpMessageProto = MergedResultMessageProto.newBuilder().setAbstractMessage(
            abstractMessage)
            .addAllMsgs(lists)
            .build();

        return mergedWarpMessageProto;
    }

    @Override
    public MergeResultMessage convert2Model(MergedResultMessageProto mergedResultMessageProto) {
        MergeResultMessage result = new MergeResultMessage();
        List<Any> anys = mergedResultMessageProto.getMsgsList();

        List<AbstractResultMessage> temp = new ArrayList<>();
        for (Any any : anys) {
            final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(
                getTypeNameFromTypeUrl(any.getTypeUrl()));
            if (any.is(clazz)) {
                try {
                    Object ob = any.unpack(clazz);
                    final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(
                        clazz.getName());
                    Object model = pbConvertor.convert2Model(ob);
                    temp.add((AbstractResultMessage)model);
                } catch (InvalidProtocolBufferException e) {
                    throw new ShouldNeverHappenException(e);
                }
            }
        }
        result.setMsgs(temp.toArray(new AbstractResultMessage[temp.size()]));

        return result;
    }

    private static String getTypeNameFromTypeUrl(
        java.lang.String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        return pos == -1 ? "" : typeUrl.substring(pos + 1);
    }
}