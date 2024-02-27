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

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.serializer.protobuf.generated.BatchResultMessageProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type batch result message protobuf convertor test.
 *
 * @since 1.5.0
 */
public class BatchResultMessageConvertorTest {

    @Test
    public void test() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();
        BranchCommitResponse branchCommitResponse = buildBranchCommitResponsePhaseTwoCommitted();
        batchResultMessage.getResultMessages().add(branchCommitResponse);
        BatchResultMessageConvertor pbConvertor = new BatchResultMessageConvertor();
        BatchResultMessageProto batchResultMessageProto = pbConvertor.convert2Proto(batchResultMessage);
        BatchResultMessage model = pbConvertor.convert2Model(batchResultMessageProto);
        BranchCommitResponse decodeModel = (BranchCommitResponse)model.getResultMessages().get(0);
        assertThat(decodeModel.getXid()).isEqualTo(branchCommitResponse.getXid());
        assertThat(decodeModel.getBranchId()).isEqualTo(branchCommitResponse.getBranchId());
        assertThat(decodeModel.getResultCode()).isEqualTo(branchCommitResponse.getResultCode());
        assertThat(decodeModel.getBranchStatus()).isEqualTo(branchCommitResponse.getBranchStatus());
    }

    private BranchCommitResponse buildBranchCommitResponsePhaseTwoCommitted() {
        final BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setBranchId(12345678L);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setXid("x1");
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);
        return branchCommitResponse;
    }
}
