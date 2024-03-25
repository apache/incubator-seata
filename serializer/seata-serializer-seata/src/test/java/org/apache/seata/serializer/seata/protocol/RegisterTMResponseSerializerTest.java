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
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Test;
import org.apache.seata.core.protocol.ProtocolConstants;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Register tm response codec test.
 *
 */
public class RegisterTMResponseSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        RegisterTMResponse registerTMResponse = new RegisterTMResponse();
        registerTMResponse.setVersion("abc");
        registerTMResponse.setExtraData("abc123");
        registerTMResponse.setIdentified(true);
        registerTMResponse.setMsg("123456");
        registerTMResponse.setResultCode(ResultCode.Failed);

        byte[] bytes = seataSerializer.serialize(registerTMResponse);

        RegisterTMResponse registerTMResponse2 = seataSerializer.deserialize(bytes);

        assertThat(registerTMResponse2.isIdentified()).isEqualTo(registerTMResponse.isIdentified());
        assertThat(registerTMResponse2.getVersion()).isEqualTo(registerTMResponse.getVersion());

//        Assert.assertEquals(registerTMResponse2.getExtraData(), registerTMResponse.getExtraData());
//        Assert.assertEquals(registerTMResponse2.getMsg(), registerTMResponse.getMsg());
//        Assert.assertEquals(registerTMResponse2.getByCode(), registerTMResponse.getByCode());
    }
}
