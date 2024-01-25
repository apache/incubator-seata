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

import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type MergedWarpMessageTest test.
 *
 */
public class MergedWarpMessageTest {

    @Test
    public void getTypeCode() {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();
        assertThat(mergedWarpMessage.getTypeCode()).isEqualTo(MessageType.TYPE_SEATA_MERGE);
    }

    @Test
    public void testToString() {
        MergedWarpMessage mergedWarpMessage = new MergedWarpMessage();

        assertEquals("SeataMergeMessage ", mergedWarpMessage.toString());

        RegisterRMResponse registerRMResponse = new RegisterRMResponse();
        registerRMResponse.setVersion("1");
        registerRMResponse.setIdentified(true);
        registerRMResponse.setResultCode(ResultCode.Failed);
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();
        registerTMResponse.setVersion("2");
        registerTMResponse.setIdentified(true);
        registerTMResponse.setResultCode(ResultCode.Success);
        mergedWarpMessage.msgs = Arrays.asList(registerRMResponse, registerTMResponse);

        assertEquals(
                "SeataMergeMessage RegisterRMResponse{version='1', extraData='null', identified=true, resultCode=Failed, msg='null'}\nRegisterTMResponse{version='2', extraData='null', identified=true, resultCode=Success, msg='null'}\n",
                mergedWarpMessage.toString()
        );
    }

    private GlobalBeginRequest buildGlobalBeginRequest() {
        final GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("xx");
        globalBeginRequest.setTimeout(3000);
        return globalBeginRequest;
    }
}
