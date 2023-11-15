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
package io.seata.serializer.seata.protocol;

import java.util.ArrayList;
import java.util.List;

import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.serializer.seata.SeataSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type batch result message codec test.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.5.0
 */
public class BatchResultMessageSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer();


    @Test
    public void testCodec() {
        BatchResultMessage batchResultMessage = new BatchResultMessage();
        final List<AbstractResultMessage> msgs = new ArrayList<>();
        final List<Integer> msgIds = new ArrayList<>();

        final BranchCommitResponse branchCommitResponse1 = buildBranchCommitResponsePhaseTwoCommitted();
        final BranchCommitResponse branchCommitResponse2 = buildBranchCommitResponsePhaseOneFailed();
        msgs.add(branchCommitResponse1);
        msgIds.add(1111);
        msgs.add(branchCommitResponse2);
        msgIds.add(2222);
        batchResultMessage.setResultMessages(msgs);
        batchResultMessage.setMsgIds(msgIds);

        byte[] body = seataSerializer.serialize(batchResultMessage);
        BatchResultMessage batchResultMessage2 = seataSerializer.deserialize(body);

        // validate msgIds
        assertThat(batchResultMessage2.getMsgIds().size()).isEqualTo(2);
        assertThat(batchResultMessage2.getMsgIds().get(0)).isEqualTo(1111);
        assertThat(batchResultMessage2.getMsgIds().get(1)).isEqualTo(2222);

        // validate msgs
        BranchCommitResponse branchCommitResponse11 = (BranchCommitResponse) batchResultMessage2.getResultMessages().get(0);
        assertThat(branchCommitResponse11.getBranchId()).isEqualTo(12345678L);
        assertThat(branchCommitResponse11.getXid()).isEqualTo("x1");
        assertThat(branchCommitResponse11.getResultCode()).isEqualTo(ResultCode.Success);
        assertThat(branchCommitResponse11.getBranchStatus()).isEqualTo(BranchStatus.PhaseTwo_Committed);
        BranchCommitResponse branchCommitResponse22 = (BranchCommitResponse) batchResultMessage2.getResultMessages().get(1);
        assertThat(branchCommitResponse22.getBranchId()).isEqualTo(87654321L);
        assertThat(branchCommitResponse22.getXid()).isEqualTo("x2");
        assertThat(branchCommitResponse11.getResultCode()).isEqualTo(ResultCode.Success);
        assertThat(branchCommitResponse22.getBranchStatus()).isEqualTo(BranchStatus.PhaseOne_Failed);
    }

    private BranchCommitResponse buildBranchCommitResponsePhaseTwoCommitted() {
        final BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setBranchId(12345678L);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setXid("x1");
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);
        return branchCommitResponse;
    }

    private BranchCommitResponse buildBranchCommitResponsePhaseOneFailed() {
        final BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
        branchCommitResponse.setBranchId(87654321L);
        branchCommitResponse.setResultCode(ResultCode.Success);
        branchCommitResponse.setXid("x2");
        branchCommitResponse.setBranchStatus(BranchStatus.PhaseOne_Failed);
        return branchCommitResponse;
    }
}
