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
package io.seata.codec.hessian;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Xin Wang
 */
public class HessianCodecTest {

    private static HessianCodec hessianCodec;

    @BeforeAll
    public static void before() {
        hessianCodec = new HessianCodec();
    }

    @Test
    public void testBranchCommitRequest() {

        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setBranchType(BranchType.AT);
        branchCommitRequest.setXid("xid");
        branchCommitRequest.setResourceId("resourceId");
        branchCommitRequest.setBranchId(20190809);
        branchCommitRequest.setApplicationData("app");

        byte[] bytes = hessianCodec.encode(branchCommitRequest);
        BranchCommitRequest t = hessianCodec.decode(bytes);

        assertThat(t.getTypeCode()).isEqualTo(branchCommitRequest.getTypeCode());
        assertThat(t.getBranchType()).isEqualTo(branchCommitRequest.getBranchType());
        assertThat(t.getXid()).isEqualTo(branchCommitRequest.getXid());
        assertThat(t.getResourceId()).isEqualTo(branchCommitRequest.getResourceId());
        assertThat(t.getBranchId()).isEqualTo(branchCommitRequest.getBranchId());
        assertThat(t.getApplicationData()).isEqualTo(branchCommitRequest.getApplicationData());

    }

    @Test
    public void testBranchCommitResponse() {

        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchCommitResponse.setBranchId(20190809);
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Done);
        branchCommitResponse.setMsg("20190809");
        branchCommitResponse.setXid("20190809");
        branchCommitResponse.setResultCode(ResultCode.Failed);

        byte[] bytes = hessianCodec.encode(branchCommitResponse);
        BranchCommitResponse t = hessianCodec.decode(bytes);

        assertThat(t.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(t.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(t.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(t.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(t.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());

    }

}
