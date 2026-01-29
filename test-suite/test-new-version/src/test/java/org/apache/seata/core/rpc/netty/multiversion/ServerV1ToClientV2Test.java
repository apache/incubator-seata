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
 * Test V1 Server with V2 Client compatibility.
 * - Server: Manual V1 construction with TestServerHandler (simulates legacy server)
 * - Client: NettyClientBootstrap (production V2 client with MultiProtocolDecoder)
 *
 * This tests client auto-downgrade - V2 client connecting to V1 server.
 */
public class ServerV1ToClientV2Test extends MultiVersionCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerV1ToClientV2Test.class);

    /**
     * Test V2 client to V1 server - success scenario.
     * V2 client should auto-detect V1 server response and handle it correctly.
     */
    @Test
    public void testServerV1ToClientV2Success() throws Exception {
        // Manual V1 server construction (simulates legacy server)
        startV1Server(8121);
        // Use production-like V2 client (NettyClientBootstrap)
        connectV2Client("127.0.0.1", 8121, 3000);

        // Step 1: Register TM
        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");

        // Step 2: Send heartbeat to verify bidirectional encode/decode
        doSendHeartbeatAndVerify();
        LOGGER.info("Heartbeat PING/PONG verified - bidirectional communication works (V1 Server + V2 Client)");
    }

    /**
     * Test V2 client to V1 server - failure scenario (auth error).
     * Note: V1 server does not include error message in response.
     */
    @Test
    public void testServerV1ToClientV2AuthFailure() throws Exception {
        startV1Server(8122);
        connectV2Client("127.0.0.1", 8122, 3000);

        RegisterTMResponse tmResponse = doSendRegister(CodecTestCheckAuthHandler.CODEC_TEST_REG_ERROR);
        Assertions.assertFalse(tmResponse.isIdentified(), "Should fail identification due to auth error");
        Assertions.assertTrue(StringUtils.isBlank(tmResponse.getMsg()), "V1 server should not include error message");
    }
}
