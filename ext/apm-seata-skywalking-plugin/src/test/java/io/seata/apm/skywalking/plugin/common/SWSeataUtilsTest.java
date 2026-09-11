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
package io.seata.apm.skywalking.plugin.common;

import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author zhaoyuguang
 */
public class SWSeataUtilsTest {

    @Test
    public void testConvertOperationName() {
        {
            RpcMessage rpcMessage = new RpcMessage();
            AbstractMessage abstractMessage = new GlobalBeginRequest();
            rpcMessage.setBody(abstractMessage);
            Assertions.assertEquals(SWSeataUtils.convertOperationName(rpcMessage), "Seata/TM/GlobalBeginRequest");
        }
        {
            RpcMessage rpcMessage = new RpcMessage();
            AbstractMessage abstractMessage = new RegisterRMRequest();
            rpcMessage.setBody(abstractMessage);
            Assertions.assertEquals(SWSeataUtils.convertOperationName(rpcMessage), "Seata/RM/RegisterRMRequest");
        }
        {
            SeataPluginConfig.Plugin.SEATA.SERVER = true;
            RpcMessage rpcMessage = new RpcMessage();
            AbstractMessage abstractMessage = new RegisterRMResponse();
            rpcMessage.setBody(abstractMessage);
            Assertions.assertEquals(SWSeataUtils.convertOperationName(rpcMessage), "Seata/TC/RegisterRMResponse");
        }
    }
}