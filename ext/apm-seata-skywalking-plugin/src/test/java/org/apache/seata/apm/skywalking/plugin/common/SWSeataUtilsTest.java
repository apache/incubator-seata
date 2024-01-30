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
package org.apache.seata.apm.skywalking.plugin.common;

import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


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
