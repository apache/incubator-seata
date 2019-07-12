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
package io.seata.codec.protobuf.convertor;

import io.seata.codec.protobuf.generated.GlobalBeginRequestProto;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalBeginRequestConvertorTest {

    @Test
    public void convert2Proto() {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTimeout(3000);
        globalBeginRequest.setTransactionName("taa");

        GlobalBeginRequestConvertor convertor = new GlobalBeginRequestConvertor();
        GlobalBeginRequestProto proto = convertor.convert2Proto(globalBeginRequest);
        GlobalBeginRequest real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(globalBeginRequest.getTypeCode());
        assertThat(real.getTimeout()).isEqualTo(globalBeginRequest.getTimeout());
        assertThat(real.getTransactionName()).isEqualTo(globalBeginRequest.getTransactionName());

    }
}