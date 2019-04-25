/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package io.seata.core.protocol.protobuf;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.convertor.MergedWarpMessageConvertor;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.serialize.ProtobufSerialzer;
import io.seata.core.protocol.serialize.ProtobufConvertManager;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan

 */
public class MergeMessageTest {

    @Test
    public void test() {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        final ArrayList<AbstractMessage> msgs = new ArrayList<>();
        final GlobalBeginRequest globalBeginRequest = buildGlobalBeginRequest();
        msgs.add(globalBeginRequest);
        mergedWarpMessage.msgs = msgs;

        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(
            globalBeginRequest.getClass().getName());

        GlobalBeginRequestProto globalBeginRequestProto = (GlobalBeginRequestProto)pbConvertor.convert2Proto(
            globalBeginRequest);

        final short typeCode = mergedWarpMessage.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        MergedWarpMessageProto mergedWarpMessageProto = MergedWarpMessageProto.newBuilder().setAbstractMessage(
            abstractMessage).addMsgs(Any.pack(globalBeginRequestProto)).build();

        byte[] bytes = ProtobufSerialzer.serializeContent(mergedWarpMessageProto);

        MergedWarpMessageProto result = ProtobufSerialzer.deserializeContent(MergedWarpMessageProto.class.getName(),
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