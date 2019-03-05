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
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;

import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * The type Branch register request test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/23
 */
public class BranchRegisterRequestTest {
    /**
     * To string test.
     *
     * @throws Exception the exception
     */
    @Test
    public void toStringTest() throws Exception {
        BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();

        branchRegisterRequest.setTransactionId(123456);
        branchRegisterRequest.setBranchType(BranchType.AT);
        branchRegisterRequest.setResourceId("resource1");
        branchRegisterRequest.setLockKey("lock_key_1");

        System.out.println(branchRegisterRequest.toString());

        Assert.assertEquals("transactionId=123456,branchType=AT,resourceId=resource1,lockKey=lock_key_1",
            branchRegisterRequest.toString());

    }


    /**
     * Test decode.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDecode() throws Exception {
        BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();

        branchRegisterRequest.setTransactionId(123456);
        branchRegisterRequest.setBranchType(BranchType.AT);
        branchRegisterRequest.setResourceId("resource1");
        branchRegisterRequest.setLockKey("lock_key_1");
        branchRegisterRequest.setApplicationData("test app data");

        byte[] encodeResult = branchRegisterRequest.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
        byteBuffer.put(encodeResult);
        byteBuffer.flip();

        BranchRegisterRequest decodeBranchRegisterRequest = new BranchRegisterRequest();
        decodeBranchRegisterRequest.decode(byteBuffer);
        System.out.println(decodeBranchRegisterRequest);
        Assert.assertEquals(branchRegisterRequest.getTransactionId(), decodeBranchRegisterRequest.getTransactionId());
        Assert.assertEquals(branchRegisterRequest.getLockKey(), decodeBranchRegisterRequest.getLockKey());
        Assert.assertEquals(branchRegisterRequest.getResourceId(), decodeBranchRegisterRequest.getResourceId());
        Assert.assertEquals(branchRegisterRequest.getApplicationData(), decodeBranchRegisterRequest.getApplicationData());
    }
}
