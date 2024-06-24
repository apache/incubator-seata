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
package org.apache.seata.serializer.seata.protocol.transaction;

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.serializer.seata.SeataSerializer;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Branch report response codec test.
 *
 */
public class BranchReportResponseSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        BranchReportResponse branchReportResponse = new BranchReportResponse();
        branchReportResponse.setMsg("abac");
        branchReportResponse.setResultCode(ResultCode.Failed);
        branchReportResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchTransactionNotExist);

        byte[] bytes = seataSerializer.serialize(branchReportResponse);

        BranchReportResponse branchReportResponse2 = seataSerializer.deserialize(bytes);

        assertThat(branchReportResponse2.getMsg()).isEqualTo(branchReportResponse.getMsg());
        assertThat(branchReportResponse2.getResultCode()).isEqualTo(branchReportResponse.getResultCode());
        assertThat(branchReportResponse2.getTransactionExceptionCode()).isEqualTo(branchReportResponse.getTransactionExceptionCode());

    }
}
