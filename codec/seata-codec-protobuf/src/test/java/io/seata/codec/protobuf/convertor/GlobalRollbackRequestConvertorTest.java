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

import io.seata.codec.protobuf.generated.GlobalRollbackRequestProto;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalRollbackRequestConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalRollbackRequest globalRollbackRequest = new GlobalRollbackRequest();
        globalRollbackRequest.setExtraData("extraData");
        globalRollbackRequest.setXid("xid");

        GlobalRollbackRequestConvertor convertor = new GlobalRollbackRequestConvertor();
        GlobalRollbackRequestProto proto = convertor.convert2Proto(
            globalRollbackRequest);
        GlobalRollbackRequest real = convertor.convert2Model(proto);
        assertThat((real.getTypeCode())).isEqualTo(globalRollbackRequest.getTypeCode());
        assertThat((real.getXid())).isEqualTo(globalRollbackRequest.getXid());
        assertThat((real.getExtraData())).isEqualTo(globalRollbackRequest.getExtraData());
    }
}