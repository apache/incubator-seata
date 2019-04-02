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

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.FragmentXID;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Assert;
import org.junit.Test;

/**
 * The type Branch commit request test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/23
 */
public class BranchCommitRequestTest {

    /**
     * To string test.
     *
     * @throws Exception the exception
     */
    @Test
    public void toStringTest() throws Exception {
        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();

        branchCommitRequest.setXid(FragmentXID.from(100));
        branchCommitRequest.setBranchId(1);
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setResourceId("resource1");
        branchCommitRequest.setApplicationData("app1");

        Assert.assertEquals("xid=<" + FragmentXID.from(100).toString() + ">,branchId=1,branchType=AT,"
            + "resourceId=resource1,applicationData=app1", branchCommitRequest.toString());

    }

    @Test
    public void testDecode() {
        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();

        branchCommitRequest.setXid(FragmentXID.from(100));
        branchCommitRequest.setBranchId(1);
        branchCommitRequest.setBranchType(BranchType.TCC);
        branchCommitRequest.setResourceId("resource1");
        branchCommitRequest.setApplicationData("app1");

        byte[] encodeResult = branchCommitRequest.encode();

        ByteBuf byteBuffer = UnpooledByteBufAllocator.DEFAULT.directBuffer(encodeResult.length);
        byteBuffer.writeBytes(encodeResult);

        BranchCommitRequest decodeBranchCommitRequest = new BranchCommitRequest();
        decodeBranchCommitRequest.decode(byteBuffer);
        System.out.println(decodeBranchCommitRequest);
        Assert.assertEquals(decodeBranchCommitRequest.getXid(), decodeBranchCommitRequest.getXid());
        Assert.assertEquals(decodeBranchCommitRequest.getBranchId(), decodeBranchCommitRequest.getBranchId());
        Assert.assertEquals(decodeBranchCommitRequest.getResourceId(), decodeBranchCommitRequest.getResourceId());
        Assert.assertEquals(decodeBranchCommitRequest.getApplicationData(), decodeBranchCommitRequest.getApplicationData());
        Assert.assertEquals(decodeBranchCommitRequest.getBranchType(), decodeBranchCommitRequest.getBranchType());
    }
}