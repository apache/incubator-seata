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
 * Test V2 Server with V1 Client compatibility.
 * - Server: MockNettyRemotingServer (production V2 server with MultiProtocolDecoder)
 * - Client: Manual V1 construction (simulates legacy client with ProtocolEncoderV1)
 *
 * This tests server backward compatibility - V2 server handling V1 client.
 */
public class ServerV2ToClientV1Test extends MultiVersionCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerV2ToClientV1Test.class);

    /**
     * Test V1 client to V2 server - success scenario.
     * V2 server should handle V1 client request and respond appropriately.
     */
    @Test
    public void testServerV2ToClientV1Success() throws Exception {
        // Use production-like V2 server (MockNettyRemotingServer)
        startV2Server(8211);
        // Use manual V1 client construction (simulates legacy client)
        connectV1Client("127.0.0.1", 8211, 3000);

        // Step 1: Register TM
        RegisterTMResponse tmResponse = doSendRegister(null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");

        // Step 2: Send heartbeat to verify bidirectional encode/decode
        doSendHeartbeatAndVerify();
        LOGGER.info("Heartbeat PING/PONG verified - bidirectional communication works (V2 Server + V1 Client)");
    }

    /**
     * Test V1 client to V2 server - second success scenario.
     * MockRegisterProcessor always returns success, so we test another success case.
     */
    @Test
    public void testServerV2ToClientV1SuccessWithExtraData() throws Exception {
        startV2Server(8212);
        connectV1Client("127.0.0.1", 8212, 3000);

        RegisterTMResponse tmResponse = doSendRegister("test-extra-data");
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified with extra data");
    }
}
