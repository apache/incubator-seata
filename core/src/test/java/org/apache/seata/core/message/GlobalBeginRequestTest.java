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
package org.apache.seata.core.message;

import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Global begin request test.
 *
 * @since 2019 /1/24
 */
public class GlobalBeginRequestTest {

    /**
     * Test to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testToString() throws Exception {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTransactionName("tran 1");

        Assertions.assertEquals("GlobalBeginRequest{transactionName='tran 1', timeout=60000}", globalBeginRequest.toString());
    }

}
