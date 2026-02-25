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
package org.apache.seata.serializer.seata.serializer;

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test for SeataSerializerV1
 */
public class SeataSerializerV1Test {

    private final SeataSerializerV1 serializer = SeataSerializerV1.getInstance();

    @Test
    public void testGetInstance() {
        SeataSerializerV1 instance1 = SeataSerializerV1.getInstance();
        SeataSerializerV1 instance2 = SeataSerializerV1.getInstance();
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    public void testProtocolVersion() {
        assertThat(serializer.protocolVersion()).isEqualTo(ProtocolConstants.VERSION_1);
    }

    @Test
    public void testSerializeAndDeserializeRegisterTMRequest() {
        RegisterTMRequest request = new RegisterTMRequest("testApp", "testGroup");
        request.setExtraData("extra");

        byte[] bytes = serializer.serialize(request);
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);

        RegisterTMRequest deserialized = serializer.deserialize(bytes);
        assertThat(deserialized.getApplicationId()).isEqualTo("testApp");
        assertThat(deserialized.getTransactionServiceGroup()).isEqualTo("testGroup");
    }

    @Test
    public void testSerializeAndDeserializeRegisterTMResponse() {
        RegisterTMResponse response = new RegisterTMResponse();
        response.setIdentified(true);
        response.setVersion("1.0.0");
        response.setResultCode(ResultCode.Success);

        byte[] bytes = serializer.serialize(response);
        assertThat(bytes).isNotNull();

        RegisterTMResponse deserialized = serializer.deserialize(bytes);
        assertThat(deserialized.isIdentified()).isTrue();
        assertThat(deserialized.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    public void testSerializeNonAbstractMessageThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            serializer.serialize("not an AbstractMessage");
        });
    }
}
