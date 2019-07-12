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
        assertThat(MessageType.TYPE_REG_RM).isEqualTo(registerRMRequest.getTypeCode());
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
