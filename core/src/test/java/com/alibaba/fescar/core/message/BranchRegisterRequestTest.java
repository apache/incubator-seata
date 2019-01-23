package com.alibaba.fescar.core.message;

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author xiajun.0706@163.com
 * @since 2019/1/23
 */
public class BranchRegisterRequestTest {
  @Test
  public void toStringTest() throws Exception{
    BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();

    branchRegisterRequest.setTransactionId(123456);
    branchRegisterRequest.setBranchType(BranchType.AT);
    branchRegisterRequest.setResourceId("resource1");
    branchRegisterRequest.setLockKey("lock_key_1");

    System.out.println(branchRegisterRequest.toString());

    Assert.assertEquals("transactionId=123456,branchType=AT,resourceId=resource1,lockKey=lock_key_1", branchRegisterRequest.toString());

  }
}
