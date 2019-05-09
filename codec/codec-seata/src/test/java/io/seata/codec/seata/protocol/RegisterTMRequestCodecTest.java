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
package io.seata.codec.seata.protocol;

import io.seata.codec.seata.SeataCodec;
import io.seata.core.protocol.RegisterTMRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Register tm request codec test.
 *
 * @author zhangsen
 * @data 2019 /5/8
 */
public class RegisterTMRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        RegisterTMRequest registerTMRequest = new RegisterTMRequest();
        registerTMRequest.setApplicationId("abc");
        registerTMRequest.setExtraData("abc123");
        registerTMRequest.setTransactionServiceGroup("def");
        registerTMRequest.setVersion("1");

        byte[] body = seataCodec.encode(registerTMRequest);

        RegisterTMRequest registerTMRequest2 = seataCodec.decode(body);

        assertThat(registerTMRequest2.getApplicationId()).isEqualTo(registerTMRequest.getApplicationId());
        assertThat(registerTMRequest2.getExtraData()).isEqualTo(registerTMRequest.getExtraData());
        assertThat(registerTMRequest2.getTransactionServiceGroup()).isEqualTo(registerTMRequest.getTransactionServiceGroup());
        assertThat(registerTMRequest2.getVersion()).isEqualTo(registerTMRequest.getVersion());
    }
}