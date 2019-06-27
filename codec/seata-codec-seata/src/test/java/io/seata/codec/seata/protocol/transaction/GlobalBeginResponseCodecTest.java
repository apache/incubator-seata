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
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * The type Global begin response codec test.
 *
 * @author zhangsen
 * @data 2019 /5/8
 */
public class GlobalBeginResponseCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionNotActive);
        globalBeginResponse.setExtraData("absd");
        globalBeginResponse.setXid("2454");
        globalBeginResponse.setResultCode(ResultCode.Failed);
        globalBeginResponse.setMsg("abcs");

        byte[] bytes = seataCodec.encode(globalBeginResponse);

        GlobalBeginResponse globalBeginResponse2 = seataCodec.decode(bytes);

        assertThat(globalBeginResponse2.getTransactionExceptionCode()).isEqualTo(globalBeginResponse.getTransactionExceptionCode());
        assertThat(globalBeginResponse2.getResultCode()).isEqualTo(globalBeginResponse.getResultCode());
        assertThat(globalBeginResponse2.getXid()).isEqualTo(globalBeginResponse.getXid());
        assertThat(globalBeginResponse2.getExtraData()).isEqualTo(globalBeginResponse.getExtraData());
        assertThat(globalBeginResponse2.getMsg()).isEqualTo(globalBeginResponse.getMsg());
    }

}