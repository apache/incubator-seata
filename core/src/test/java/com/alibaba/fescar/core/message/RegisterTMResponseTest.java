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

import com.alibaba.fescar.core.protocol.RegisterTMResponse;
import com.alibaba.fescar.core.protocol.ResultCode;

import org.junit.Assert;
import org.junit.Test;

public class RegisterTMResponseTest {

  @Test
  public void testToString() throws Exception{
    RegisterTMResponse registerTMResponse = new RegisterTMResponse();

    registerTMResponse.setVersion("1");
    registerTMResponse.setIdentified(true);
    registerTMResponse.setResultCode(ResultCode.Success);

    Assert.assertEquals("version=1,extraData=null,identified=true,resultCode=Success,msg=null", registerTMResponse.toString());

  }
}
