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

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;

import org.junit.Test;

/**
 * The type Codec test.
 */
public class CodecTest {

    /**
     * Test a.
     */
    @Test
    public void testA() {

        long tid = 232323L;
        long bid = 43554545L;

        BranchRegisterResponse response = new BranchRegisterResponse();
        response.setResultCode(ResultCode.Failed);
        response.setTransactionId(tid);
        response.setBranchId(bid);

        byte[] bytes = response.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();

        BranchRegisterResponse rs = new BranchRegisterResponse();
        rs.decode(byteBuffer);

        System.out.println(rs.getTransactionId());
    }
}
