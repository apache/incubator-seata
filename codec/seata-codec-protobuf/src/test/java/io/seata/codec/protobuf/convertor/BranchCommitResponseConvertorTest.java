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
package io.seata.codec.protobuf.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.codec.protobuf.generated.BranchCommitResponseProto;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class BranchCommitResponseConvertorTest {

    @Test
    public void convert2Proto() {

        BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setMsg("xx");
        branchCommitResponse.setXid("xid");
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
        branchCommitResponse.setBranchId(123);

        BranchCommitResponseConvertor convertor = new BranchCommitResponseConvertor();
        BranchCommitResponseProto proto = convertor.convert2Proto(branchCommitResponse);
        BranchCommitResponse real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(branchCommitResponse.getTypeCode());
        assertThat(real.getMsg()).isEqualTo(branchCommitResponse.getMsg());
        assertThat(real.getXid()).isEqualTo(branchCommitResponse.getXid());
        assertThat(real.getTransactionExceptionCode()).isEqualTo(branchCommitResponse.getTransactionExceptionCode());
        assertThat(real.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
        assertThat(real.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());
    }
}