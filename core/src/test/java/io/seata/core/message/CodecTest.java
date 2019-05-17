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

import java.nio.ByteBuffer;

import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchRegisterResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Codec test.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class CodecTest {

    /**
     * Test a.
     */
    @Test
    public void testA() {
        long bid = 43554545L;
        BranchRegisterResponse response = new BranchRegisterResponse();
        response.setResultCode(ResultCode.Failed);
        response.setBranchId(bid);
        byte[] bytes = response.encode();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();

        BranchRegisterResponse rs = new BranchRegisterResponse();
        rs.decode(byteBuffer);

        Assertions.assertEquals(response.getBranchId(), rs.getBranchId());
        Assertions.assertEquals(response.getResultCode(), rs.getResultCode());
    }
}
