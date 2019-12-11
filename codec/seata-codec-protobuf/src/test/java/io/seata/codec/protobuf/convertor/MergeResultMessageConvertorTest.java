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

import io.seata.codec.protobuf.generated.MergedResultMessageProto;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class MergeResultMessageConvertorTest {

    @Test
    public void convert2Proto() {

        MergeResultMessage mergeResultMessage = new MergeResultMessage();
        AbstractResultMessage[] msgs = new AbstractResultMessage[1];
        final GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();
        globalCommitResponse.setGlobalStatus(GlobalStatus.AsyncCommitting);
        globalCommitResponse.setMsg("msg");
        globalCommitResponse.setResultCode(ResultCode.Failed);
        globalCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchRegisterFailed);
        msgs[0] = globalCommitResponse;
        mergeResultMessage.setMsgs(msgs);

        MergeResultMessageConvertor convertor = new MergeResultMessageConvertor();
        MergedResultMessageProto proto = convertor.convert2Proto(mergeResultMessage);
        MergeResultMessage real = convertor.convert2Model(proto);

        GlobalCommitResponse realObj = (GlobalCommitResponse)real.getMsgs()[0];

        assertThat((realObj.getTypeCode())).isEqualTo(globalCommitResponse.getTypeCode());
        assertThat((realObj.getMsg())).isEqualTo(globalCommitResponse.getMsg());
        assertThat((realObj.getResultCode())).isEqualTo(globalCommitResponse.getResultCode());
        assertThat((realObj.getTransactionExceptionCode())).isEqualTo(
            globalCommitResponse.getTransactionExceptionCode());
    }
}