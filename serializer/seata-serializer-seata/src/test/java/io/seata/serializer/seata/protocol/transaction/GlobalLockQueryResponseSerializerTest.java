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
package io.seata.serializer.seata.protocol.transaction;

import io.seata.serializer.seata.SeataSerializer;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Global lock query response codec test.
 *
 * @author zhangsen
 */
public class GlobalLockQueryResponseSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalLockQueryResponse globalLockQueryResponse = new GlobalLockQueryResponse();
        globalLockQueryResponse.setLockable(true);
        globalLockQueryResponse.setMsg("aa");
        globalLockQueryResponse.setResultCode(ResultCode.Failed);
        globalLockQueryResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalTransactionStatusInvalid);

        byte[] bytes = seataSerializer.serialize(globalLockQueryResponse);

        GlobalLockQueryResponse globalLockQueryResponse2 = seataSerializer.deserialize(bytes);

        assertThat(globalLockQueryResponse2.isLockable()).isEqualTo(globalLockQueryResponse.isLockable());
        assertThat(globalLockQueryResponse2.getResultCode()).isEqualTo(globalLockQueryResponse.getResultCode());
        assertThat(globalLockQueryResponse2.getTransactionExceptionCode()).isEqualTo(globalLockQueryResponse.getTransactionExceptionCode());
        assertThat(globalLockQueryResponse2.getMsg()).isEqualTo(globalLockQueryResponse.getMsg());
    }

}
