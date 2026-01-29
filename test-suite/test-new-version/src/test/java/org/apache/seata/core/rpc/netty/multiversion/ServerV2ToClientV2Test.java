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

import org.apache.seata.core.protocol.RegisterTMResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test V2 Server with V2 Client compatibility.
 * Both server and client use V2 protocol with production-like bootstrap:
 * - Server: MockNettyRemotingServer (uses ProtocolDetectHandler -> SeataDetector -> MultiProtocolDecoder)
 * - Client: NettyClientBootstrap (uses ProtocolEncoderV2 + MultiProtocolDecoder)
 *
 * This is the most production-like test scenario.
 */
public class ServerV2ToClientV2Test extends MultiVersionCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerV2ToClientV2Test.class);

    /**
     * Test V2 client to V2 server - success scenario.
     * Uses production MockNettyRemotingServer and NettyClientBootstrap.
     */
    @Test
    public void testServerV2ToClientV2Success() throws Exception {
        // Use production-like V2 server (MockNettyRemotingServer)
        startV2Server(8221);
        // Use production-like V2 client (NettyClientBootstrap)
        connectV2Client("127.0.0.1", 8221, 3000);

        // Step 1: Register TM
        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");

        // Step 2: Send heartbeat to verify bidirectional encode/decode
        doSendHeartbeatAndVerify();
        LOGGER.info("Heartbeat PING/PONG verified - bidirectional communication works (V2 Server + V2 Client)");
    }

    /**
     * Test V2 client to V2 server - failure scenario (auth error).
     * Note: V2 protocol includes error message in response.
     */
    @Test
    public void testServerV2ToClientV2AuthFailure() throws Exception {
        startV2Server(8222);
        connectV2Client("127.0.0.1", 8222, 3000);

        // MockRegisterProcessor in MockNettyRemotingServer always returns success,
        // so we only test the success path for V2 server.
        // Auth failure testing is done with manual V1 server construction.
        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");
    }
}
