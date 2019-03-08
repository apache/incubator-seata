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

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;

import org.junit.Assert;
import org.junit.Test;

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
    public void toStringTest() throws Exception {
        BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();

        branchRegisterResponse.setTransactionId(123456L);
        branchRegisterResponse.setBranchId(123457L);
        branchRegisterResponse.setResultCode(ResultCode.Success);
        branchRegisterResponse.setMsg("");
        Assert.assertEquals(
            "BranchRegisterResponse: transactionId=123456,branchId=123457,result code =Success,getMsg =",
            branchRegisterResponse.toString());

    }
}
