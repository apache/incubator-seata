/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.protobuf;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.convertor.MergedWarpMessageConvertor;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.serialize.FrameSerialzer;
import io.seata.core.protocol.serialize.ProtobufConvertManager;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author bystander
 * @version : MergeMessageTest.java, v 0.1 2019年04月24日 07:16 bystander Exp $
 */
public class MergeMessageTest {

    @Test
    public void test() {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        final ArrayList<AbstractMessage> msgs = new ArrayList<>();
        final GlobalBeginRequest globalBeginRequest = buildGlobalBeginRequest();
        msgs.add(globalBeginRequest);
        mergedWarpMessage.msgs = msgs;

        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetcConvertor(
            globalBeginRequest.getClass().getName());

        GlobalBeginRequestProto globalBeginRequestProto = (GlobalBeginRequestProto)pbConvertor.convert2Proto(
            globalBeginRequest);

        final short typeCode = mergedWarpMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        MergedWarpMessageProto mergedWarpMessageProto = MergedWarpMessageProto.newBuilder().setAbstractMessage(
            abstractMessage).addMsgs(Any.pack(globalBeginRequestProto)).build();

        byte[] bytes = FrameSerialzer.serializeContent(mergedWarpMessageProto);

        MergedWarpMessageProto result = FrameSerialzer.deserializeContent(MergedWarpMessageProto.class.getName(),
            bytes);

        List<Any> anys = result.getMsgsList();

        assertThat(anys.size()).isEqualTo(1);

        MergedWarpMessageConvertor convertor = new MergedWarpMessageConvertor();
        MergedWarpMessage model = convertor.convert2Model(result);

        GlobalBeginRequest decodeModel = (GlobalBeginRequest)model.msgs.get(0);
        assertThat(decodeModel.getTransactionName()).isEqualTo(
            globalBeginRequest.getTransactionName());
        assertThat(decodeModel.getTimeout()).isEqualTo(globalBeginRequest.getTimeout());
        assertThat(
            decodeModel.getTypeCode()).isEqualTo(globalBeginRequest.getTypeCode());

    }

    private GlobalBeginRequest buildGlobalBeginRequest() {
        final GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);
        return globalBeginRequest;
    }
}