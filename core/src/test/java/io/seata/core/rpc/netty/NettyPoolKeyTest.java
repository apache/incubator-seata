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
package io.seata.core.rpc.netty;

import io.seata.core.protocol.RegisterRMRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author: jimin.jm@alibaba-inc.com
 * @date 2019/04/12
 */
public class NettyPoolKeyTest {

    private NettyPoolKey nettyPoolKey;
    private static final NettyPoolKey.TransactionRole RM_ROLE = NettyPoolKey.TransactionRole.RMROLE;
    private static final NettyPoolKey.TransactionRole TM_ROLE = NettyPoolKey.TransactionRole.TMROLE;
    private static final String ADDRESS1 = "127.0.0.1:8091";
    private static final String ADDRESS2 = "127.0.0.1:8092";
    private static final RegisterRMRequest MSG1 = new RegisterRMRequest("applicationId1", "transactionServiceGroup1");
    private static final RegisterRMRequest MSG2 = new RegisterRMRequest("applicationId2", "transactionServiceGroup2");

    @BeforeEach
    public void init() {
        nettyPoolKey = new NettyPoolKey(RM_ROLE, ADDRESS1, MSG1);
    }

    @Test
    public void getTransactionRole() {
        Assertions.assertEquals(nettyPoolKey.getTransactionRole(), RM_ROLE);
    }

    @Test
    public void setTransactionRole() {
        nettyPoolKey.setTransactionRole(TM_ROLE);
        Assertions.assertEquals(nettyPoolKey.getTransactionRole(), TM_ROLE);
    }

    @Test
    public void getAddress() {
        Assertions.assertEquals(nettyPoolKey.getAddress(), ADDRESS1);
    }

    @Test
    public void setAddress() {
        nettyPoolKey.setAddress(ADDRESS2);
        Assertions.assertEquals(nettyPoolKey.getAddress(), ADDRESS2);
    }

    @Test
    public void getMessage() {
        Assertions.assertEquals(nettyPoolKey.getMessage(), MSG1);
    }

    @Test
    public void setMessage() {
        nettyPoolKey.setMessage(MSG2);
        Assertions.assertEquals(nettyPoolKey.getMessage(), MSG2);
    }

    @Test
    public void testToString() {
        String expectStr = "transactionRole:RMROLE,address:127.0.0.1:8091,msg:< " + MSG1.toString() + " >";
        Assertions.assertEquals(nettyPoolKey.toString(), expectStr);
    }
}
