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

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author xiajun.0706@163.com
 * @since 2019/1/24
 */
public class BranchReportRequestTest {

    @Test
    public void testToString() throws Exception {
        BranchReportRequest branchReportRequest = new BranchReportRequest();

        branchReportRequest.setTransactionId(1249853);
        branchReportRequest.setBranchId(3);
        branchReportRequest.setResourceId("resource003");
        branchReportRequest.setStatus(BranchStatus.PhaseOne_Timeout);
        branchReportRequest.setApplicationData("test app data");

        Assert.assertEquals(
            "transactionId=1249853,branchId=3,resourceId=resource003,status=PhaseOne_Timeout,applicationData=test app"
                + " data",
            branchReportRequest.toString());
    }

    @Test
    public void testEncode() throws Exception {
        BranchReportRequest branchReportRequest = new BranchReportRequest();

        branchReportRequest.setTransactionId(1249853);
        branchReportRequest.setBranchId(3);
        branchReportRequest.setResourceId("resource003");
        branchReportRequest.setStatus(BranchStatus.PhaseOne_Timeout);
        branchReportRequest.setApplicationData("test app data");

        byte[] encodeResult = branchReportRequest.encode();
        System.out.println(encodeResult);
        String encodeResultStr = Arrays.toString(encodeResult);
        System.out.println(encodeResultStr);

        Assert.assertEquals(
            "[0, 0, 0, 0, 0, 19, 18, 61, 0, 0, 0, 0, 0, 0, 0, 3, 4, 0, 11, 114, 101, 115, 111, 117, 114, 99, 101, 48,"
                + " 48, 51, 0, 0, 0, 13, 116, 101, 115, 116, 32, 97, 112, 112, 32, 100, 97, 116, 97]",
            encodeResultStr);
    }

    @Test
    public void testDecode() throws Exception {
        BranchReportRequest branchReportRequest = new BranchReportRequest();

        branchReportRequest.setTransactionId(1249853);
        branchReportRequest.setBranchId(3);
        branchReportRequest.setResourceId("resource003");
        branchReportRequest.setStatus(BranchStatus.PhaseOne_Timeout);
        branchReportRequest.setApplicationData("test app data");

        byte[] encodeResult = branchReportRequest.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
        byteBuffer.put(encodeResult);
        byteBuffer.flip();

        BranchReportRequest decodeBranchReportRequest = new BranchReportRequest();
        decodeBranchReportRequest.decode(byteBuffer);
        System.out.println(decodeBranchReportRequest);
        Assert.assertEquals(branchReportRequest.getTransactionId(), decodeBranchReportRequest.getTransactionId());
        Assert.assertEquals(branchReportRequest.getBranchId(), decodeBranchReportRequest.getBranchId());
        Assert.assertEquals(branchReportRequest.getResourceId(), decodeBranchReportRequest.getResourceId());
        Assert.assertEquals(branchReportRequest.getStatus(), decodeBranchReportRequest.getStatus());
        Assert.assertEquals(branchReportRequest.getApplicationData(), decodeBranchReportRequest.getApplicationData());
    }
}