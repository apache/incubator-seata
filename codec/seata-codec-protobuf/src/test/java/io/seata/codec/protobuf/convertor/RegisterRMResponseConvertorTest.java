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

import io.seata.codec.protobuf.generated.RegisterRMResponseProto;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class RegisterRMResponseConvertorTest {

    @Test
    public void convert2Proto() {

        RegisterRMResponse registerRMResponse = new RegisterRMResponse();
        registerRMResponse.setResultCode(ResultCode.Failed);
        registerRMResponse.setMsg("msg");
        registerRMResponse.setIdentified(true);
        registerRMResponse.setVersion("11");
        registerRMResponse.setExtraData("extraData");
        RegisterRMResponseConvertor convertor = new RegisterRMResponseConvertor();
        RegisterRMResponseProto proto = convertor.convert2Proto(registerRMResponse);
        RegisterRMResponse real = convertor.convert2Model(proto);

        assertThat((real.getTypeCode())).isEqualTo(registerRMResponse.getTypeCode());
        assertThat((real.getMsg())).isEqualTo(registerRMResponse.getMsg());
        assertThat((real.getResultCode())).isEqualTo(registerRMResponse.getResultCode());
        assertThat((real.isIdentified())).isEqualTo(registerRMResponse.isIdentified());
        assertThat((real.getVersion())).isEqualTo(registerRMResponse.getVersion());
        assertThat((real.getExtraData())).isEqualTo(registerRMResponse.getExtraData());

    }
}