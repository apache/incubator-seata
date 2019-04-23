/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.protobuf;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.seata.core.convertor.GlobalBeginRequestConvertor;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.serialize.FrameSerialzer;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

        GlobalBeginRequestProto globalBeginRequestProto = GlobalBeginRequestConvertor.convert2Proto(globalBeginRequest);

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

        for (Any any : anys) {
            if (any.is(GlobalBeginRequestProto.class)) {
                try {
                    Object ob = any.unpack(GlobalBeginRequestProto.class);
                    assertThat(ob instanceof GlobalBeginRequestProto).isEqualTo(true);

                    GlobalBeginRequestProto decodeGlobalBeginRequest = (GlobalBeginRequestProto)ob;
                    assertThat(decodeGlobalBeginRequest.getTransactionName()).isEqualTo(
                        globalBeginRequest.getTransactionName());
                    assertThat(decodeGlobalBeginRequest.getTimeout()).isEqualTo(globalBeginRequest.getTimeout());
                    assertThat(
                        decodeGlobalBeginRequest.getAbstractTransactionRequest().getAbstractMessage().getMessageType()
                            .getNumber()).isEqualTo(globalBeginRequest.getTypeCode());

                } catch (InvalidProtocolBufferException e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    private GlobalBeginRequest buildGlobalBeginRequest() {
        final GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);
        return globalBeginRequest;
    }
}