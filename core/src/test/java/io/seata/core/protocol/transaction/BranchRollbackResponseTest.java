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
package io.seata.core.protocol.transaction;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author: jimin.jm@alibaba-inc.com
 * @date 2019/04/16
 */
public class BranchRollbackResponseTest {
    @Test
    public void toStringTest() {
        BranchRollbackResponse branchRollbackResponse = new BranchRollbackResponse();
        branchRollbackResponse.setXid("127.0.0.1:8091:123456");
        branchRollbackResponse.setBranchId(2345678L);
        branchRollbackResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchRollbackResponse.setResultCode(ResultCode.Success);
        branchRollbackResponse.setMsg("");
        Assertions.assertEquals(
            "xid=127.0.0.1:8091:123456,branchId=2345678,branchStatus=PhaseOne_Done,result code =Success,getMsg =",
            branchRollbackResponse.toString());

    }

    @Test
    public void testEncodeDecode() {
        BranchRollbackResponse branchRollbackResponse = new BranchRollbackResponse();

        branchRollbackResponse.setXid("127.0.0.1:9999:39875642");
        branchRollbackResponse.setBranchId(10241024L);
        branchRollbackResponse.setResultCode(ResultCode.Success);
        branchRollbackResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);

        byte[] encodeResult = branchRollbackResponse.encode();

        ByteBuf byteBuffer = UnpooledByteBufAllocator.DEFAULT.directBuffer(encodeResult.length);
        byteBuffer.writeBytes(encodeResult);

        BranchRollbackResponse decodeBranchRollbackResponse = new BranchRollbackResponse();
        decodeBranchRollbackResponse.decode(byteBuffer);
        Assertions.assertEquals(decodeBranchRollbackResponse.getXid(), branchRollbackResponse.getXid());
        Assertions.assertEquals(decodeBranchRollbackResponse.getBranchId(), branchRollbackResponse.getBranchId());
        Assertions.assertEquals(decodeBranchRollbackResponse.getResultCode(), branchRollbackResponse.getResultCode());
        Assertions.assertEquals(decodeBranchRollbackResponse.getBranchStatus(), branchRollbackResponse.getBranchStatus());
        Assertions.assertEquals(decodeBranchRollbackResponse.getTransactionExceptionCode(),
            branchRollbackResponse.getTransactionExceptionCode());
        Assertions.assertEquals(decodeBranchRollbackResponse.getMsg(), branchRollbackResponse.getMsg());
    }

}
