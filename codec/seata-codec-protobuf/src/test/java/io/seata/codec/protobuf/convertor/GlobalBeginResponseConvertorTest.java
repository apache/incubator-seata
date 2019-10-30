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

import io.seata.codec.protobuf.generated.GlobalBeginResponseProto;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalBeginResponseConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();

        globalBeginResponse.setResultCode(ResultCode.Failed);
        globalBeginResponse.setMsg("msg");
        globalBeginResponse.setExtraData("extraData");
        globalBeginResponse.setXid("xid");
        globalBeginResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchRollbackFailed_Retriable);

        GlobalBeginResponseConvertor convertor = new GlobalBeginResponseConvertor();
        GlobalBeginResponseProto proto = convertor.convert2Proto(globalBeginResponse);
        GlobalBeginResponse real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(globalBeginResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(globalBeginResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(globalBeginResponse.getResultCode());
        assertThat((real.getTransactionExceptionCode())).isEqualTo(globalBeginResponse.getTransactionExceptionCode());
    }
}