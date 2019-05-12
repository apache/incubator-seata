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
import java.util.Arrays;

import io.seata.core.protocol.transaction.GlobalBeginRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Global begin request test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/24
 */
public class GlobalBeginRequestTest {

    /**
     * Test to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testToString() throws Exception {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("tran 1");
        System.out.println(globalBeginRequest.toString());

        Assertions.assertEquals("timeout=60000,transactionName=tran 1", globalBeginRequest.toString());
    }

    /**
     * Test encode.
     *
     * @throws Exception the exception
     */
    @Test
    public void testEncode() throws Exception {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("tran 1");

        byte[] encodeResult = globalBeginRequest.encode();
        String encodeResultStr = Arrays.toString(encodeResult);

        Assertions.assertEquals("[0, 0, -22, 96, 0, 6, 116, 114, 97, 110, 32, 49]", encodeResultStr);
    }

    /**
     * Test decode.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDecode() throws Exception {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("tran 1");

        byte[] encodeResult = globalBeginRequest.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
        byteBuffer.put(encodeResult);
        byteBuffer.flip();

        GlobalBeginRequest decodeGlobalBeginRequest = new GlobalBeginRequest();
        decodeGlobalBeginRequest.decode(byteBuffer);
        System.out.println(decodeGlobalBeginRequest);
        Assertions.assertEquals(globalBeginRequest.getTimeout(), decodeGlobalBeginRequest.getTimeout());
        Assertions.assertEquals(globalBeginRequest.getTransactionName(), decodeGlobalBeginRequest.getTransactionName());
    }
}
