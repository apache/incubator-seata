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
package org.apache.seata.serializer.seata.protocol.v2;

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.serializer.seata.SeataSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for RegisterTMResponseCodecV2 and RegisterRMResponseCodecV2
 */
public class RegisterResponseCodecV2Test {

    private final SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION_2);

    @Test
    public void testRegisterTMResponseCodecV2() {
        RegisterTMResponse response = new RegisterTMResponse();
        response.setIdentified(true);
        response.setVersion("2.0.0");
        response.setResultCode(ResultCode.Success);
        response.setMsg("success");

        byte[] bytes = seataSerializer.serialize(response);
        assertThat(bytes).isNotNull();

        RegisterTMResponse deserialized = seataSerializer.deserialize(bytes);
        assertThat(deserialized.isIdentified()).isTrue();
        assertThat(deserialized.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    public void testRegisterTMResponseCodecV2WithNullVersion() {
        RegisterTMResponse response = new RegisterTMResponse();
        response.setIdentified(false);
        response.setVersion(null);
        response.setResultCode(ResultCode.Failed);

        byte[] bytes = seataSerializer.serialize(response);
        RegisterTMResponse deserialized = seataSerializer.deserialize(bytes);
        assertThat(deserialized.isIdentified()).isFalse();
    }

    @Test
    public void testRegisterRMResponseCodecV2() {
        RegisterRMResponse response = new RegisterRMResponse();
        response.setIdentified(true);
        response.setVersion("2.0.0");
        response.setResultCode(ResultCode.Success);

        byte[] bytes = seataSerializer.serialize(response);
        assertThat(bytes).isNotNull();

        RegisterRMResponse deserialized = seataSerializer.deserialize(bytes);
        assertThat(deserialized.isIdentified()).isTrue();
        assertThat(deserialized.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    public void testRegisterTMResponseCodecV2GetMessageClassType() {
        RegisterTMResponseCodecV2 codec = new RegisterTMResponseCodecV2();
        assertThat(codec.getMessageClassType()).isEqualTo(RegisterTMResponse.class);
    }

    @Test
    public void testRegisterRMResponseCodecV2GetMessageClassType() {
        RegisterRMResponseCodecV2 codec = new RegisterRMResponseCodecV2();
        assertThat(codec.getMessageClassType()).isEqualTo(RegisterRMResponse.class);
    }
}
