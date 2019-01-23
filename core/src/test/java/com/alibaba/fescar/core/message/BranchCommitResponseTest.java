package com.alibaba.fescar.core.message;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitResponse;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author xiajun.0706@163.com
 * @since 2019/1/23
 */
public class BranchCommitResponseTest {
  @Test
  public void toStringTest() throws Exception{
    BranchCommitResponse branchCommitResponse = new BranchCommitResponse();

    branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
    branchCommitResponse.setResultCode(ResultCode.Success);
    branchCommitResponse.setMsg("");

    System.out.println(branchCommitResponse.toString());

    Assert.assertEquals("branchStatus=PhaseOne_Done,result code =Success,getMsg =", branchCommitResponse.toString());

  }
}
