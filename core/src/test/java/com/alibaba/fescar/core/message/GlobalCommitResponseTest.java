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

import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitResponse;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * The type Global commit response test.
 *
 * @author xiajun.0706 @163.com
 * @since 2019 /1/24
 */
public class GlobalCommitResponseTest {

    /**
     * Test to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testToString() throws Exception {
        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();

        globalCommitResponse.setGlobalStatus(GlobalStatus.Committed);
        globalCommitResponse.setResultCode(ResultCode.Success);
        globalCommitResponse.setMsg("OK");

        System.out.println(globalCommitResponse.toString());

        Assert.assertEquals("globalStatus=Committed,ResultCode=Success,Msg=OK", globalCommitResponse.toString());
    }

    /**
     * Test encode.
     *
     * @throws Exception the exception
     */
    @Test
    public void testEncode() throws Exception {
        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();

        globalCommitResponse.setGlobalStatus(GlobalStatus.Committed);
        globalCommitResponse.setResultCode(ResultCode.Success);
        globalCommitResponse.setMsg("OK");

        System.out.println(globalCommitResponse.toString());

        byte[] encodeResult = globalCommitResponse.encode();
        System.out.println(encodeResult);
        String encodeResultStr = Arrays.toString(encodeResult);
        System.out.println(encodeResultStr);

        Assert.assertEquals("[1, 0, 8]", encodeResultStr);
    }

    /**
     * Test decode normal.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDecodeNormal() throws Exception {
        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();

        globalCommitResponse.setGlobalStatus(GlobalStatus.Committed);
        globalCommitResponse.setResultCode(ResultCode.Success);
        globalCommitResponse.setMsg("OK");

        System.out.println(globalCommitResponse.toString());

        byte[] encodeResult = globalCommitResponse.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
        byteBuffer.put(encodeResult);
        byteBuffer.flip();
        GlobalCommitResponse decodeGlobalCommitResponse = new GlobalCommitResponse();
        decodeGlobalCommitResponse.decode(byteBuffer);

        System.out.println(decodeGlobalCommitResponse.toString());

        Assert.assertEquals(globalCommitResponse.getGlobalStatus(), decodeGlobalCommitResponse.getGlobalStatus());
        Assert.assertEquals(globalCommitResponse.getResultCode(), decodeGlobalCommitResponse.getResultCode());
        //success response do not have msg
        Assert.assertTrue(StringUtils.isBlank(decodeGlobalCommitResponse.getMsg()));
    }

    /**
     * Test decode exception.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDecodeException() throws Exception {
        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();

        globalCommitResponse.setGlobalStatus(GlobalStatus.CommitFailed);
        globalCommitResponse.setResultCode(ResultCode.Failed);
        globalCommitResponse.setMsg("error happened");

        System.out.println(globalCommitResponse.toString());

        byte[] encodeResult = globalCommitResponse.encode();

        ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
        byteBuffer.put(encodeResult);
        byteBuffer.flip();
        GlobalCommitResponse decodeGlobalCommitResponse = new GlobalCommitResponse();
        decodeGlobalCommitResponse.decode(byteBuffer);

        System.out.println(decodeGlobalCommitResponse.toString());

        Assert.assertEquals(globalCommitResponse.getGlobalStatus(), decodeGlobalCommitResponse.getGlobalStatus());
        Assert.assertEquals(globalCommitResponse.getResultCode(), decodeGlobalCommitResponse.getResultCode());
        Assert.assertEquals(globalCommitResponse.getMsg(), decodeGlobalCommitResponse.getMsg());
    }
}