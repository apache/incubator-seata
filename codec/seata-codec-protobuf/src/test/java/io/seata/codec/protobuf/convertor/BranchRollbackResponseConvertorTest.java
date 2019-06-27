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
import io.seata.codec.protobuf.generated.BranchRollbackResponseProto;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class BranchRollbackResponseConvertorTest {

    @Test
    public void convert2Proto() {

        BranchRollbackResponse branchRollbackResponse = new BranchRollbackResponse();
        branchRollbackResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);
        branchRollbackResponse.setResultCode(ResultCode.Success);
        branchRollbackResponse.setMsg("xx");
        branchRollbackResponse.setXid("xid");
        branchRollbackResponse.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
        branchRollbackResponse.setBranchId(123);

        BranchRollbackResponseConvertor convertor = new BranchRollbackResponseConvertor();
        BranchRollbackResponseProto proto = convertor.convert2Proto(
            branchRollbackResponse);
        BranchRollbackResponse real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(branchRollbackResponse.getTypeCode());
        assertThat(real.getMsg()).isEqualTo(branchRollbackResponse.getMsg());
        assertThat(real.getXid()).isEqualTo(branchRollbackResponse.getXid());
        assertThat(real.getTransactionExceptionCode()).isEqualTo(branchRollbackResponse.getTransactionExceptionCode());
        assertThat(real.getBranchStatus()).isEqualTo(branchRollbackResponse.getBranchStatus());
        assertThat(real.getResultCode()).isEqualTo(branchRollbackResponse.getResultCode());
    }
}