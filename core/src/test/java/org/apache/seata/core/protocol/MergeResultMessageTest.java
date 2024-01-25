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
package org.apache.seata.core.protocol;

import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type MergeResultMessage test.
 *
 */
public class MergeResultMessageTest {

    @Test
    public void getAndSetMsgs() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        final AbstractResultMessage[] msgs = new AbstractResultMessage[1];
        final GlobalBeginResponse globalBeginResponse = buildGlobalBeginResponse();
        msgs[0] = globalBeginResponse;
        mergeResultMessage.setMsgs(msgs);
        assertThat(globalBeginResponse).isEqualTo(mergeResultMessage.getMsgs()[0]);
    }

    @Test
    public void getTypeCode() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        assertThat(MessageType.TYPE_SEATA_MERGE_RESULT).isEqualTo(mergeResultMessage.getTypeCode());
    }

    @Test
    public void testToString() {
        MergeResultMessage mergeResultMessage = new MergeResultMessage();

        assertEquals("MergeResultMessage ", mergeResultMessage.toString());

        RegisterRMResponse registerRMResponse = new RegisterRMResponse();
        registerRMResponse.setVersion("1");
        registerRMResponse.setIdentified(true);
        registerRMResponse.setResultCode(ResultCode.Failed);
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();
        registerTMResponse.setVersion("2");
        registerTMResponse.setIdentified(true);
        registerTMResponse.setResultCode(ResultCode.Success);
        mergeResultMessage.msgs = new AbstractResultMessage[] {registerRMResponse, registerTMResponse};

        Assertions.assertEquals(
                "MergeResultMessage RegisterRMResponse{version='1', extraData='null', identified=true, resultCode=Failed, msg='null'}\nRegisterTMResponse{version='2', extraData='null', identified=true, resultCode=Success, msg='null'}\n",
                mergeResultMessage.toString()
        );
    }

    private GlobalBeginResponse buildGlobalBeginResponse() {
        final GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setExtraData("data");
        globalBeginResponse.setMsg("success");
        globalBeginResponse.setResultCode(ResultCode.Success);
        return globalBeginResponse;
    }

}
