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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test V1 Server and V2 Client compatibility (client auto-downgrade)
 */
public class S1C2Test extends MultiVersionCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(S1C2Test.class);

    /**
     * Test V2 client to V1 server - success scenario (client auto-downgrade)
     */
    @Test
    public void testV2ClientToV1ServerSuccess() throws Exception {
        RegisterTMResponse tmResponse = doSendRegister(8211, null);
        Assertions.assertTrue(tmResponse.isIdentified(), "Should be successfully identified");
    }

    /**
     * Test V2 client to V1 server - failure scenario (auth error)
     */
    @Test
    public void testV2ClientToV1ServerFailure() throws Exception {
        RegisterTMResponse tmResponse = doSendRegister(8212, CodecTestCheckAuthHandler.CODEC_TEST_REG_ERROR);
        Assertions.assertFalse(tmResponse.isIdentified(), "Should fail identification due to auth error");
        Assertions.assertTrue(StringUtils.isBlank(tmResponse.getMsg()), "Error message should not be present");
    }

    @NotNull
    private RegisterTMResponse doSendRegister(int port, String extraData) throws InterruptedException {
        startV1Server(port);
        connectV2Client("127.0.0.1", port, 3000);
        return doSendRegister(extraData);
    }
}