package com.alibaba.fescar.core.message;

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitRequest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author xiajun.0706@163.com
 * @since 2019/1/23
 */
public class BranchCommitRequestTest {

  @Test
  public void toStringTest() throws Exception{
    BranchCommitRequest branchCommitRequest = new BranchCommitRequest();

    branchCommitRequest.setXid("127.0.0.1:9999:39875642");
    branchCommitRequest.setBranchId(1);
    branchCommitRequest.setBranchType(BranchType.AT);
    branchCommitRequest.setResourceId("resource1");
    branchCommitRequest.setApplicationData("app1");

    Assert.assertEquals("xid=127.0.0.1:9999:39875642,branchId=1,branchType=AT,"
                        + "resourceId=resource1,applicationData=app1", branchCommitRequest.toString());

  }
}
