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

import io.seata.codec.protobuf.generated.BranchReportResponseProto;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchReportResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class BranchReportResponseConvertorTest {

    @Test
    public void convert2Proto() {

        BranchReportResponse branchReportResponse = new BranchReportResponse();
        branchReportResponse.setMsg("msg");
        branchReportResponse.setResultCode(ResultCode.Failed);
        branchReportResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotExist);
        BranchReportResponseConvertor convertor = new BranchReportResponseConvertor();
        BranchReportResponseProto proto = convertor.convert2Proto(branchReportResponse);
        BranchReportResponse real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(branchReportResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(branchReportResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(branchReportResponse.getResultCode());
        assertThat((real.getTransactionExceptionCode())).isEqualTo(branchReportResponse.getTransactionExceptionCode());

    }
}