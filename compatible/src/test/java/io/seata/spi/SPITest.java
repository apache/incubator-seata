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
package io.seata.spi;

import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SPITest {

    @Test
    public void testRmSPIOrder() {
        DefaultResourceManager defaultResourceManager = DefaultResourceManager.get();
        ResourceManager resourceManager = defaultResourceManager.getResourceManager(BranchType.SAGA);
        Assertions.assertNotNull(resourceManager);
        Assertions.assertEquals("io.seata.saga.rm.SagaResourceManager", resourceManager.getClass().getName());
    }
}
