/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.MergedResultMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.serialize.ProtobufConvertManager;

/**
 * @author bystander
 * @version : MergeResultMessageConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class MergeResultMessageConvertor implements PbConvertor<MergeResultMessage, MergedResultMessageProto> {
    @Override
    public MergedResultMessageProto convert2Proto(MergeResultMessage mergeResultMessage) {
        final short typeCode = mergeResultMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        List<Any> lists = new ArrayList<>();
        for (AbstractMessage msg : mergeResultMessage.msgs) {
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetcConvertor(
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
            final Class clazz = ProtobufConvertManager.getInstance().fetchClass(
                getTypeNameFromTypeUrl(any.getTypeUrl()));
            if (any.is(clazz)) {
                try {
                    Object ob = any.unpack(clazz);
                    temp.add((AbstractResultMessage)ob);
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