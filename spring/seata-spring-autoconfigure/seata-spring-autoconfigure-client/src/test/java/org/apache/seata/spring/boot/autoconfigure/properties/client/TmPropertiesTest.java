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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TmPropertiesTest {

    @Test
    public void testTmProperties() {
        TmProperties tmProperties = new TmProperties();
        tmProperties.setCommitRetryCount(1);
        Assertions.assertEquals(1, tmProperties.getCommitRetryCount());

        tmProperties.setRollbackRetryCount(1);
        Assertions.assertEquals(1, tmProperties.getRollbackRetryCount());

        tmProperties.setDefaultGlobalTransactionTimeout(1);
        Assertions.assertEquals(1, tmProperties.getDefaultGlobalTransactionTimeout());

        tmProperties.setDegradeCheck(true);
        Assertions.assertTrue(tmProperties.isDegradeCheck());

        tmProperties.setDegradeCheckPeriod(1);
        Assertions.assertEquals(1, tmProperties.getDegradeCheckPeriod());

        tmProperties.setDegradeCheckAllowTimes(1);
        Assertions.assertEquals(1, tmProperties.getDegradeCheckAllowTimes());

        tmProperties.setInterceptorOrder(1);
        Assertions.assertEquals(1, tmProperties.getInterceptorOrder());
    }
}
