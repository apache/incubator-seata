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
package org.apache.seata.core.rpc.hook;

import org.apache.seata.common.rpc.RpcStatus;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StatusRpcHookTest {

    @Test
    void testDoBeforeRequest() {
        String service = "192.168.1.1:8080";
        RpcStatus status = RpcStatus.getStatus(service);
        assertNotNull(status, "RpcStatus should not be null");

        StatusRpcHook hook = new StatusRpcHook();
        hook.doBeforeRequest("192.168.1.1:8080", new RpcMessage());

        assertEquals(1, status.getActive(), "Active count should be incremented");

    }

    @Test
    void testDoAfterResponse() {
        String service = "192.168.2.1:8080";
        RpcStatus status = RpcStatus.getStatus(service);
        assertNotNull(status, "RpcStatus should not be null");

        StatusRpcHook hook = new StatusRpcHook();
        hook.doBeforeRequest("192.168.2.1:8080", new RpcMessage());

        assertEquals(1, status.getActive(), "Active count should be incremented");

        hook.doAfterResponse("192.168.2.1:8080", new RpcMessage(),null);

        assertEquals(0, status.getActive(), "Active count should be decremented");
        assertEquals(1, status.getTotal(), "Active count should be incremented");
    }
}
