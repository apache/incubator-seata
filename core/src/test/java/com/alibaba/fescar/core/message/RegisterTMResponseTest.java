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
