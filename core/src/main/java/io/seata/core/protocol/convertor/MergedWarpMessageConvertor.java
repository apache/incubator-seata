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
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.MergedWarpMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.serialize.ProtobufConvertManager;

/**
 * @author bystander
 * @version : MergedWarpMessageConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class MergedWarpMessageConvertor implements PbConvertor<MergedWarpMessage, MergedWarpMessageProto> {

    @Override
    public MergedWarpMessageProto convert2Proto(MergedWarpMessage mergedWarpMessage) {

        final short typeCode = mergedWarpMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        List<Any> lists = new ArrayList<>();
        for (AbstractMessage msg : mergedWarpMessage.msgs) {
            final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetcConvertor(
                msg.getClass().getName());
            lists.add(Any.pack((Message)pbConvertor.convert2Proto(msg)));
        }

        MergedWarpMessageProto mergedWarpMessageProto = MergedWarpMessageProto.newBuilder().setAbstractMessage(
            abstractMessage)
            .addAllMsgs(lists)
            .addAllMsgIds(mergedWarpMessage.msgIds)
            .build();

        return mergedWarpMessageProto;

    }

    @Override
    public MergedWarpMessage convert2Model(MergedWarpMessageProto mergedWarpMessageProto) {
        MergedWarpMessage result = new MergedWarpMessage();
        List<Any> anys = mergedWarpMessageProto.getMsgsList();
        for (Any any : anys) {
            final Class clazz = ProtobufConvertManager.getInstance().fetchClass(
                getTypeNameFromTypeUrl(any.getTypeUrl()));
            if (any.is(clazz)) {
                try {
                    Object ob = any.unpack(clazz);
                    result.msgs.add((AbstractMessage)ob);
                } catch (InvalidProtocolBufferException e) {
                    throw new ShouldNeverHappenException(e);
                }
            }
        }
        result.msgIds = mergedWarpMessageProto.getMsgIdsList();

        return result;
    }

    private static String getTypeNameFromTypeUrl(
        java.lang.String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        return pos == -1 ? "" : typeUrl.substring(pos + 1);
    }
}