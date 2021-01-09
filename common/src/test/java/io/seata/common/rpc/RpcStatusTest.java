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

package io.seata.common.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The state statistics test.
 *
 * @author ph3636
 */
public class RpcStatusTest {

    public static final String SERVICE = "127.0.0.1:80";

    @Test
    public void getStatus() {
        RpcStatus rpcStatus1 = RpcStatus.getStatus(SERVICE);
        Assertions.assertNotNull(rpcStatus1);
        RpcStatus rpcStatus2 = RpcStatus.getStatus(SERVICE);
        Assertions.assertEquals(rpcStatus1, rpcStatus2);
    }

    @Test
    public void removeStatus() {
        RpcStatus old = RpcStatus.getStatus(SERVICE);
        RpcStatus.removeStatus(SERVICE);
        Assertions.assertNotEquals(RpcStatus.getStatus(SERVICE), old);
    }

    @Test
    public void beginCount() {
        RpcStatus.beginCount(SERVICE);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getActive(), 1);
    }

    @Test
    public void endCount() {
        RpcStatus.endCount(SERVICE);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getActive(), 0);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getTotal(), 1);
    }
}
