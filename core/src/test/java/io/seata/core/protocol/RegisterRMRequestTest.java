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
package io.seata.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * The type RegisterRMRequest test.
 *
 * @author leizhiyuan
 */
public class RegisterRMRequestTest {

    @Test
    public void getAndSetResourceIds() {
        RegisterRMRequest registerRMRequest = new RegisterRMRequest();
        final String resourceIds = "r1,r2";
        registerRMRequest.setResourceIds(resourceIds);
        assertThat(resourceIds).isEqualTo(registerRMRequest.getResourceIds());
    }

    @Test
    public void getTypeCode() {
        RegisterRMRequest registerRMRequest = new RegisterRMRequest();
        assertThat(AbstractMessage.TYPE_REG_RM).isEqualTo(registerRMRequest.getTypeCode());
    }


    @Test
    public void encode() {
        byte[] expect = new byte[]{0, 1, 49, 0, 3, 97, 112, 112, 0, 5, 103, 114, 111, 117, 112, 0, 5, 101, 120, 116, 114, 97, 0, 0, 0, 5, 114, 49, 44, 114, 50};
        RegisterRMRequest registerRMRequest = buildRegisterRMRequest();
        byte[] result = registerRMRequest.encode();
        assertThat(expect).isEqualTo(result);
    }


    @Test
    public void decode() {
        byte[] result = new byte[]{0, 1, 49, 0, 3, 97, 112, 112, 0, 5, 103, 114, 111, 117, 112, 0, 5, 101, 120, 116, 114, 97, 0, 0, 0, 5, 114, 49, 44, 114, 50};
        RegisterRMRequest registerRMRequest = buildRegisterRMRequest();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        RegisterRMRequest decodeResult = new RegisterRMRequest();
        decodeResult.decode(buffer.writeBytes(result));
        assertThat(decodeResult.getTypeCode()).isEqualTo(registerRMRequest.getTypeCode());
        assertThat(decodeResult.getResourceIds()).isEqualTo(registerRMRequest.getResourceIds());
        assertThat(decodeResult.getApplicationId()).isEqualTo(registerRMRequest.getApplicationId());
        assertThat(decodeResult.getExtraData()).isEqualTo(registerRMRequest.getExtraData());
        assertThat(decodeResult.getTransactionServiceGroup()).isEqualTo(registerRMRequest.getTransactionServiceGroup());
        assertThat(decodeResult.getVersion()).isEqualTo(registerRMRequest.getVersion());
    }


    private RegisterRMRequest buildRegisterRMRequest() {

        RegisterRMRequest registerRMRequest = new RegisterRMRequest();
        final String resourceIds = "r1,r2";
        registerRMRequest.setResourceIds(resourceIds);
        registerRMRequest.setApplicationId("app");
        registerRMRequest.setExtraData("extra");
        registerRMRequest.setTransactionServiceGroup("group");
        registerRMRequest.setVersion("1");
        return registerRMRequest;
    }
}
