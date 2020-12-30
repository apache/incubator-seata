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
package io.seata.tm;

import io.seata.core.rpc.netty.TmNettyRemotingClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for tmClient
 * @author Ifdevil
 */
public class TMClientTest {

    private static final String APPLICATION_ID = "my_app_test";
    private static final String SERVICE_GROUP = "my_test_tx_group";

    @Test
    public void testInit(){
        TMClient.init(APPLICATION_ID,SERVICE_GROUP);
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance();
        Assertions.assertEquals(tmNettyRemotingClient.getTransactionServiceGroup(),SERVICE_GROUP);
    }
}
