/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.serializer.protobuf.generated.BranchRollbackResponseProto;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


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
