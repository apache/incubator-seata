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
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * The type Register rm response codec test.
 *
 * @author zhangsen
 */
public class RegisterRMResponseCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        RegisterRMResponse registerRMResponse = new RegisterRMResponse();
        registerRMResponse.setExtraData("abc123");
        registerRMResponse.setIdentified(true);
        registerRMResponse.setMsg("123456");
        registerRMResponse.setVersion("12");
        registerRMResponse.setResultCode(ResultCode.Failed);

        byte[] body = seataCodec.encode(registerRMResponse);

        RegisterRMResponse registerRMRespons2 = seataCodec.decode(body);

        assertThat(registerRMRespons2.isIdentified()).isEqualTo(registerRMResponse.isIdentified());
        assertThat(registerRMRespons2.getVersion()).isEqualTo(registerRMResponse.getVersion());

//        Assert.assertEquals(registerRMRespons2.getExtraData(), registerRMResponse.getExtraData());
//        Assert.assertEquals(registerRMRespons2.getMsg(), registerRMResponse.getMsg());
//        Assert.assertEquals(registerRMRespons2.getByCode(), registerRMResponse.getByCode());
    }
}
