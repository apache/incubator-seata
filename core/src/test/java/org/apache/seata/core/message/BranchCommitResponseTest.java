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

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch commit response test.
 *
 * @since 2019 /1/23
 */
public class BranchCommitResponseTest {
    /**
     * To string test.
     */
    @Test
    public void toStringTest() {
        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setXid("127.0.0.1:8091:123456");
        branchCommitResponse.setBranchId(2345678L);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setMsg("");
        Assertions.assertEquals(
            "BranchCommitResponse{xid='127.0.0.1:8091:123456', branchId=2345678, branchStatus=PhaseOne_Done, resultCode=Success, msg=''}",
            branchCommitResponse.toString());

    }

}
