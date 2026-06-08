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
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.UnregisterRMResponse;
import org.apache.seata.serializer.protobuf.generated.UnregisterRMResponseProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnregisterRMResponseConvertorTest {

    @Test
    public void convert2ProtoTest() {
        UnregisterRMResponse unregisterRMResponse = new UnregisterRMResponse();
        unregisterRMResponse.setResultCode(ResultCode.Failed);
        unregisterRMResponse.setMsg("msg");
        unregisterRMResponse.setIdentified(true);
        unregisterRMResponse.setVersion("11");
        unregisterRMResponse.setExtraData("extraData");

        UnregisterRMResponseConvertor convertor = new UnregisterRMResponseConvertor();
        UnregisterRMResponseProto proto = convertor.convert2Proto(unregisterRMResponse);
        UnregisterRMResponse real = convertor.convert2Model(proto);

        assertThat((real.getTypeCode())).isEqualTo(unregisterRMResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(unregisterRMResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(unregisterRMResponse.getResultCode());
        assertThat((real.isIdentified())).isEqualTo(unregisterRMResponse.isIdentified());
        assertThat((real.getVersion())).isEqualTo(unregisterRMResponse.getVersion());
        assertThat((real.getExtraData())).isEqualTo(unregisterRMResponse.getExtraData());
    }
}
