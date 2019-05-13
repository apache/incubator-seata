/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.message;

import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch commit response test.
 *
 * @author xiajun.0706 @163.com
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
            "xid=127.0.0.1:8091:123456,branchId=2345678,branchStatus=PhaseOne_Done,result code =Success,getMsg =",
            branchCommitResponse.toString());

    }

    @Test
    public void testEncodeDecode() {
        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();

        branchCommitResponse.setXid("127.0.0.1:9999:39875642");
        branchCommitResponse.setBranchId(10241024L);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);

        byte[] encodeResult = branchCommitResponse.encode();

        ByteBuf byteBuffer = UnpooledByteBufAllocator.DEFAULT.directBuffer(encodeResult.length);
        byteBuffer.writeBytes(encodeResult);

        BranchCommitResponse decodeBranchCommitResponse = new BranchCommitResponse();
        decodeBranchCommitResponse.decode(byteBuffer);
        Assertions.assertEquals(decodeBranchCommitResponse.getXid(), branchCommitResponse.getXid());
        Assertions.assertEquals(decodeBranchCommitResponse.getBranchId(), branchCommitResponse.getBranchId());
        Assertions.assertEquals(decodeBranchCommitResponse.getResultCode(), branchCommitResponse.getResultCode());
        Assertions.assertEquals(decodeBranchCommitResponse.getBranchStatus(), branchCommitResponse.getBranchStatus());
        Assertions.assertEquals(decodeBranchCommitResponse.getTransactionExceptionCode(),
            branchCommitResponse.getTransactionExceptionCode());
        Assertions.assertEquals(decodeBranchCommitResponse.getMsg(), branchCommitResponse.getMsg());
    }
}
