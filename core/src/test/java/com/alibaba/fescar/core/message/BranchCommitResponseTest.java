/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.fescar.core.message;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitResponse;

import org.junit.Assert;
import org.junit.Test;

/**
 * The type Branch commit response test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/23
 */
public class BranchCommitResponseTest {
    /**
     * To string test.
     *
     */
    @Test
    public void toStringTest() {
        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setXid("127.0.0.1:8091:123456");
        branchCommitResponse.setBranchId(2345678L);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setMsg("");
        Assert.assertEquals(
            "xid=127.0.0.1:8091:123456,branchId=2345678,branchStatus=PhaseOne_Done,result code =Success,getMsg =",
            branchCommitResponse.toString());

    }
}