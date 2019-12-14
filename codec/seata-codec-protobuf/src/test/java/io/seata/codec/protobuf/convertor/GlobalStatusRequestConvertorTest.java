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

import io.seata.codec.protobuf.generated.GlobalStatusRequestProto;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalStatusRequestConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalStatusRequest globalStatusRequest = new GlobalStatusRequest();
        globalStatusRequest.setExtraData("extraData");
        globalStatusRequest.setXid("xid");
        GlobalStatusRequestConvertor convertor = new GlobalStatusRequestConvertor();
        GlobalStatusRequestProto proto = convertor.convert2Proto(
            globalStatusRequest);
        GlobalStatusRequest real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(globalStatusRequest.getTypeCode());
        assertThat((real.getXid())).isEqualTo(globalStatusRequest.getXid());
        assertThat((real.getExtraData())).isEqualTo(globalStatusRequest.getExtraData());

    }
}