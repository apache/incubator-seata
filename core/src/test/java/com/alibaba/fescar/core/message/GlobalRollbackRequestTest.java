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

import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackRequest;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author xiajun.0706@163.com
 * @since 2019/1/24
 */
public class GlobalRollbackRequestTest {

  @Test
  public void testToString() throws Exception{
    GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();

    globalRollbackRequest.setTransactionId(1249853);
    globalRollbackRequest.setExtraData("test_extra_data");

    System.out.println(globalRollbackRequest.toString());

    Assert.assertEquals("transactionId=1249853,extraData=test_extra_data", globalRollbackRequest.toString());
  }

  @Test
  public void testEncode() throws Exception{
    GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();

    globalRollbackRequest.setTransactionId(1249853);
    globalRollbackRequest.setExtraData("test_extra_data");

    System.out.println(globalRollbackRequest.toString());

    byte[] encodeResult = globalRollbackRequest.encode();
    System.out.println(encodeResult);
    String encodeResultStr = Arrays.toString(encodeResult);
    System.out.println(encodeResultStr);


    Assert.assertEquals("[0, 0, 0, 0, 0, 19, 18, 61, 0, 15, 116, 101, 115, 116, 95, 101, 120, 116, 114, 97, 95, 100, 97, 116, 97]", encodeResultStr);
  }

  @Test
  public void testDecode() throws Exception{
    GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();

    globalRollbackRequest.setTransactionId(1249853L);
    globalRollbackRequest.setExtraData("test_extra_data");

    byte[] encodeResult = globalRollbackRequest.encode();

    ByteBuffer byteBuffer = ByteBuffer.allocate(encodeResult.length);
    byteBuffer.put(encodeResult);
    byteBuffer.flip();

    GlobalRollbackRequest decodeGlobalRollbackRequest = new GlobalRollbackRequest();
    decodeGlobalRollbackRequest.decode(byteBuffer);
    System.out.println(decodeGlobalRollbackRequest);
    Assert.assertEquals(globalRollbackRequest.getTransactionId(), decodeGlobalRollbackRequest.getTransactionId());
    Assert.assertEquals(globalRollbackRequest.getExtraData(), decodeGlobalRollbackRequest.getExtraData());
  }
}