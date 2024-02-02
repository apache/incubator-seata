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

import org.apache.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BranchRegisterResponseConvertorTest {

    @Test
    public void convert2Proto() {

        BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotActive);
        branchRegisterResponse.setResultCode(ResultCode.Failed);
        branchRegisterResponse.setMsg("msg");
        branchRegisterResponse.setBranchId(123);

        BranchRegisterResponseConvertor convertor = new BranchRegisterResponseConvertor();
        BranchRegisterResponseProto proto = convertor.convert2Proto(
            branchRegisterResponse);

        BranchRegisterResponse real = convertor.convert2Model(proto);

        assertThat(real.getTransactionExceptionCode()).isEqualTo(branchRegisterResponse.getTransactionExceptionCode());
        assertThat(real.getResultCode()).isEqualTo(branchRegisterResponse.getResultCode());
        assertThat(real.getMsg()).isEqualTo(branchRegisterResponse.getMsg());
        assertThat(real.getBranchId()).isEqualTo(branchRegisterResponse.getBranchId());
    }
}
