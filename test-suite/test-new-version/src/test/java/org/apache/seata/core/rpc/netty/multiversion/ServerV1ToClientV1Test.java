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
package org.apache.seata.core.rpc.netty.multiversion;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.rpc.netty.CodecTestCheckAuthHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test V1 Server with V1 Client compatibility.
 * Both server and client use V1 protocol (manual construction to simulate legacy systems).
 * - Server: Manual V1 construction with TestServerHandler
 * - Client: Manual V1 construction with ProtocolEncoderV1
 */
public class ServerV1ToClientV1Test extends MultiVersionCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerV1ToClientV1Test.class);

    /**
     * Test V1 client to V1 server - success scenario.
     * Both sides use manual construction to simulate legacy V1 systems.
     */
    @Test
    public void testServerV1ToClientV1Success() throws Exception {
        // Manual V1 server construction (simulates legacy server)
        startV1Server(8111);
        // Manual V1 client construction (simulates legacy client)
        connectV1Client("127.0.0.1", 8111, 3000);

        // Step 1: Register TM
        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");

        // Step 2: Send heartbeat to verify bidirectional encode/decode
        doSendHeartbeatAndVerify();
        LOGGER.info("Heartbeat PING/PONG verified - bidirectional communication works (V1 Server + V1 Client)");
    }

    /**
     * Test V1 client to V1 server - failure scenario (auth error).
     * Note: V1 protocol does not include error message in response.
     */
    @Test
    public void testServerV1ToClientV1AuthFailure() throws Exception {
        startV1Server(8112);
        connectV1Client("127.0.0.1", 8112, 3000);

        RegisterTMResponse tmResponse = doSendRegister(CodecTestCheckAuthHandler.CODEC_TEST_REG_ERROR);
        Assertions.assertFalse(tmResponse.isIdentified(), "Should fail identification due to auth error");
        Assertions.assertTrue(StringUtils.isBlank(tmResponse.getMsg()), "V1 protocol should not include error message");
    }
}
