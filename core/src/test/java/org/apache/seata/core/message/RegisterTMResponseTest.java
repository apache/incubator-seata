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
package org.apache.seata.core.message;

import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Register tm response test.
 */
public class RegisterTMResponseTest {

    /**
     * Test to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testToString() throws Exception {
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();
        registerTMResponse.setVersion("1");
        registerTMResponse.setIdentified(true);
        registerTMResponse.setResultCode(ResultCode.Success);
        Assertions.assertEquals("RegisterTMResponse{version='1', extraData='null', identified=true, resultCode=Success, msg='null'}",
                registerTMResponse.toString());
    }

    @Test
    public void getTypeCode() {
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();

        Assertions.assertEquals(MessageType.TYPE_REG_CLT_RESULT, registerTMResponse.getTypeCode());
    }

    @Test
    public void isIdentified() {
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();
        Assertions.assertTrue(registerTMResponse.isIdentified()); // default to true

        RegisterTMResponse registerTMResp = new RegisterTMResponse(false);
        Assertions.assertFalse(registerTMResp.isIdentified());
    }
}
