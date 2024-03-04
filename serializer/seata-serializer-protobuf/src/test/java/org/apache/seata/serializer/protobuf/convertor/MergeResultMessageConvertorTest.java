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

import org.apache.seata.serializer.protobuf.generated.MergedResultMessageProto;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


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
