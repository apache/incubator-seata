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

import org.apache.seata.serializer.protobuf.generated.BranchReportResponseProto;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


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
