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

import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch register response test.
 */
public class BranchRegisterResponseTest {

    /**
     * To string test.
     *
     * @throws Exception the exception
     */
    @Test
    public void toStringTest() {
        BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();
        branchRegisterResponse.setBranchId(123457L);
        branchRegisterResponse.setResultCode(ResultCode.Success);
        branchRegisterResponse.setMsg("");
        Assertions.assertEquals(
            "BranchRegisterResponse{branchId=123457, resultCode=Success, msg=''}",
            branchRegisterResponse.toString());

    }
}
