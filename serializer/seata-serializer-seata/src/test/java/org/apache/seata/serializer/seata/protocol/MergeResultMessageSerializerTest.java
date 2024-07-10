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
package org.apache.seata.serializer.seata.protocol;

import org.apache.seata.serializer.seata.SeataSerializer;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.jupiter.api.Test;
import org.apache.seata.core.protocol.ProtocolConstants;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Merge result message codec test.
 *
 */
public class MergeResultMessageSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        final AbstractResultMessage[] msgs = new AbstractResultMessage[2];
        final GlobalBeginResponse globalBeginResponse1 = buildGlobalBeginResponse("a1");
        final GlobalBeginResponse globalBeginResponse2 = buildGlobalBeginResponse("a2");
        msgs[0] = globalBeginResponse1;
        msgs[1] = globalBeginResponse2;
        mergeResultMessage.setMsgs(msgs);

        byte[] body = seataSerializer.serialize(mergeResultMessage);

        MergeResultMessage mergeResultMessage2 = seataSerializer.deserialize(body);
        assertThat(mergeResultMessage2.msgs.length).isEqualTo(mergeResultMessage.msgs.length);

        GlobalBeginResponse globalBeginResponse21 = (GlobalBeginResponse) mergeResultMessage2.msgs[0];
        assertThat(globalBeginResponse21.getXid()).isEqualTo(globalBeginResponse1.getXid());
        assertThat(globalBeginResponse21.getExtraData()).isEqualTo(globalBeginResponse1.getExtraData());
        assertThat(globalBeginResponse21.getMsg()).isEqualTo(globalBeginResponse1.getMsg());
        assertThat(globalBeginResponse21.getResultCode()).isEqualTo(globalBeginResponse1.getResultCode());
        assertThat(globalBeginResponse21.getTransactionExceptionCode()).isEqualTo(globalBeginResponse1.getTransactionExceptionCode());


        GlobalBeginResponse globalBeginResponse22 = (GlobalBeginResponse) mergeResultMessage2.msgs[1];
        assertThat(globalBeginResponse22.getXid()).isEqualTo(globalBeginResponse2.getXid());
        assertThat(globalBeginResponse22.getExtraData()).isEqualTo(globalBeginResponse2.getExtraData());
        assertThat(globalBeginResponse22.getMsg()).isEqualTo(globalBeginResponse2.getMsg());
        assertThat(globalBeginResponse22.getResultCode()).isEqualTo(globalBeginResponse2.getResultCode());
        assertThat(globalBeginResponse22.getTransactionExceptionCode()).isEqualTo(globalBeginResponse2.getTransactionExceptionCode());

    }

    private GlobalBeginResponse buildGlobalBeginResponse(String xid) {
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid(xid);
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Failed);
        globalBeginResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        return globalBeginResponse;
    }
}
