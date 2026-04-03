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

import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.UnregisterRMResponse;
import org.apache.seata.serializer.seata.SeataSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Unregister rm response codec test.
 */
public class UnregisterRMResponseSerializerTest {

    SeataSerializer seataSerializer = new SeataSerializer(ProtocolConstants.VERSION);

    @Test
    public void codecTest() {
        UnregisterRMResponse unregisterRMResponse = new UnregisterRMResponse();
        unregisterRMResponse.setExtraData("abc123");
        unregisterRMResponse.setIdentified(true);
        unregisterRMResponse.setMsg("123456");
        unregisterRMResponse.setVersion("12");
        unregisterRMResponse.setResultCode(ResultCode.Failed);

        byte[] body = seataSerializer.serialize(unregisterRMResponse);

        UnregisterRMResponse deserialized = seataSerializer.deserialize(body);

        assertThat(deserialized.isIdentified()).isEqualTo(unregisterRMResponse.isIdentified());
        assertThat(deserialized.getVersion()).isEqualTo(unregisterRMResponse.getVersion());
    }
}
