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
package io.seata.serializer.protobuf.convertor;

import io.seata.serializer.protobuf.generated.GlobalStatusResponseProto;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalStatusResponseConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalStatusResponse globalStatusResponse = new GlobalStatusResponse();
        globalStatusResponse.setGlobalStatus(GlobalStatus.AsyncCommitting);
        globalStatusResponse.setMsg("msg");
        globalStatusResponse.setResultCode(ResultCode.Failed);
        globalStatusResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchRegisterFailed);
        GlobalStatusResponseConvertor convertor = new GlobalStatusResponseConvertor();
        GlobalStatusResponseProto proto = convertor.convert2Proto(
            globalStatusResponse);
        GlobalStatusResponse real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(globalStatusResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(globalStatusResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(globalStatusResponse.getResultCode());
        assertThat((real.getTransactionExceptionCode())).isEqualTo(
            globalStatusResponse.getTransactionExceptionCode());

    }
}