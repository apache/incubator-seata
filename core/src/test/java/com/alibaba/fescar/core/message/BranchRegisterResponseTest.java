package com.alibaba.fescar.core.message;

import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;

import org.junit.Assert;
import org.junit.Test;

public class BranchRegisterResponseTest {

  @Test
  public void toStringTest() throws Exception{
    BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();

    branchRegisterResponse.setTransactionId(123456L);
    branchRegisterResponse.setBranchId(123457L);
    branchRegisterResponse.setResultCode(ResultCode.Success);
    branchRegisterResponse.setMsg("");

    System.out.println(branchRegisterResponse.toString());

    Assert.assertEquals("BranchRegisterResponse: transactionId=123456,branchId=123457,result code =Success,getMsg =", branchRegisterResponse.toString());

  }
}
