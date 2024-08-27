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
package org.apache.seata.serializer.seata.protocol;

import org.apache.seata.serializer.seata.SeataSerializer;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Test;
import org.apache.seata.core.protocol.ProtocolConstants;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * The type Register rm response codec test.
 *
 */
public class RegisterRMResponseSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

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

        byte[] body = seataSerializer.serialize(registerRMResponse);

        RegisterRMResponse registerRMRespons2 = seataSerializer.deserialize(body);

        assertThat(registerRMRespons2.isIdentified()).isEqualTo(registerRMResponse.isIdentified());
        assertThat(registerRMRespons2.getVersion()).isEqualTo(registerRMResponse.getVersion());

//        Assert.assertEquals(registerRMRespons2.getExtraData(), registerRMResponse.getExtraData());
//        Assert.assertEquals(registerRMRespons2.getMsg(), registerRMResponse.getMsg());
//        Assert.assertEquals(registerRMRespons2.getByCode(), registerRMResponse.getByCode());
    }
}
