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
import io.seata.core.protocol.RegisterRMRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Register rm request codec test.
 *
 * @author zhangsen
 * @data 2019 /5/8
 */
public class RegisterRMRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec() {
        RegisterRMRequest registerRMRequest = new RegisterRMRequest();
        registerRMRequest.setResourceIds("a1,a2");
        registerRMRequest.setApplicationId("abc");
        registerRMRequest.setExtraData("abc124");
        registerRMRequest.setTransactionServiceGroup("def");
        registerRMRequest.setVersion("1");

        byte[] body = seataCodec.encode(registerRMRequest);

        RegisterRMRequest registerRMRequest2 = seataCodec.decode(body);
        assertThat(registerRMRequest2.getResourceIds()).isEqualTo(registerRMRequest.getResourceIds());
        assertThat(registerRMRequest2.getExtraData()).isEqualTo(registerRMRequest.getExtraData());
        assertThat(registerRMRequest2.getApplicationId()).isEqualTo(registerRMRequest.getApplicationId());
        assertThat(registerRMRequest2.getVersion()).isEqualTo(registerRMRequest.getVersion());
        assertThat(registerRMRequest2.getTransactionServiceGroup()).isEqualTo(registerRMRequest.getTransactionServiceGroup());

    }

}