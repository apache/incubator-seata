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
package org.apache.seata.tm.api.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * the type SuspendedResourcesHolder
 */
public class SuspendedResourcesHolderTest {

    private final static String DEFAULT_XID = "1234567890";

    @Test
    void testIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SuspendedResourcesHolder(null);
        });
    }

    @Test
    void getXidTest() {
        SuspendedResourcesHolder suspendedResourcesHolder = new SuspendedResourcesHolder(DEFAULT_XID);
        Assertions.assertEquals(DEFAULT_XID, suspendedResourcesHolder.getXid());
    }
}
