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

import org.apache.seata.core.protocol.UnregisterRMRequest;
import org.apache.seata.serializer.protobuf.generated.UnregisterRMRequestProto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnregisterRMRequestConvertorTest {

    @Test
    public void convert2ProtoTest() {
        UnregisterRMRequest unregisterRMRequest = new UnregisterRMRequest();
        unregisterRMRequest.setResourceIds("res1");
        unregisterRMRequest.setVersion("123");
        unregisterRMRequest.setTransactionServiceGroup("group");
        unregisterRMRequest.setExtraData("extraData");
        unregisterRMRequest.setApplicationId("appId");

        UnregisterRMRequestConvertor convertor = new UnregisterRMRequestConvertor();
        UnregisterRMRequestProto proto = convertor.convert2Proto(unregisterRMRequest);
        UnregisterRMRequest real = convertor.convert2Model(proto);

        assertThat((real.getTypeCode())).isEqualTo(unregisterRMRequest.getTypeCode());
        assertThat((real.getResourceIds())).isEqualTo(unregisterRMRequest.getResourceIds());
        assertThat((real.getVersion())).isEqualTo(unregisterRMRequest.getVersion());
        assertThat((real.getTransactionServiceGroup())).isEqualTo(unregisterRMRequest.getTransactionServiceGroup());
        assertThat((real.getExtraData())).isEqualTo(unregisterRMRequest.getExtraData());
        assertThat((real.getApplicationId())).isEqualTo(unregisterRMRequest.getApplicationId());
    }
}
