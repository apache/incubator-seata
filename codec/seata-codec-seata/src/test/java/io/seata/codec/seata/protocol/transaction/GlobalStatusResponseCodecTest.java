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
package io.seata.codec.seata.protocol.transaction;

import io.seata.codec.seata.SeataCodec;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Global status response codec test.
 *
 * @author zhangsen
 * @data 2019 /5/8
 */
public class GlobalStatusResponseCodecTest {
    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalStatusResponse globalStatusResponse = new GlobalStatusResponse();
        globalStatusResponse.setGlobalStatus(GlobalStatus.CommitRetrying);
        globalStatusResponse.setMsg("aaaa");
        globalStatusResponse.setResultCode(ResultCode.Failed);
        globalStatusResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotExist);

        byte[] bytes = seataCodec.encode(globalStatusResponse);

        GlobalStatusResponse globalStatusResponse2 = seataCodec.decode(bytes);
        assertThat(globalStatusResponse2.getGlobalStatus()).isEqualTo(globalStatusResponse.getGlobalStatus());
        assertThat(globalStatusResponse2.getMsg()).isEqualTo(globalStatusResponse.getMsg());
        assertThat(globalStatusResponse2.getTransactionExceptionCode()).isEqualTo(globalStatusResponse.getTransactionExceptionCode());
        assertThat(globalStatusResponse2.getResultCode()).isEqualTo(globalStatusResponse.getResultCode());
    }
}