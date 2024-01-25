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
package org.apache.seata.metrics.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RegistryType}
 *
 */
class RegistryTypeTest {

    @Test
    void getName() {
        Assertions.assertEquals("compact", RegistryType.COMPACT.getName());
    }

    @Test
    void getType_invalidTypeName_throwException() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> RegistryType.getType("comp"));
    }

    @Test
    void getType_validTypeNameLowerCase() {
        Assertions.assertEquals(RegistryType.COMPACT, RegistryType.getType("compact"));
    }

    @Test
    void getType_validTypeNameMixedCase() {
        Assertions.assertEquals(RegistryType.COMPACT, RegistryType.getType("compAcT"));
    }

    @Test
    void values() {
        Assertions.assertArrayEquals(new RegistryType[]{RegistryType.COMPACT}, RegistryType.values());
    }
}
