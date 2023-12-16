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
package io.seata.metrics.exporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExporterType}
 *
 * @author Mia0451
 */
class ExporterTypeTest {

    @Test
    void values() {
        Assertions.assertArrayEquals(new ExporterType[]{ExporterType.PROMETHEUS}, ExporterType.values());
    }

    @Test
    void getName() {
        Assertions.assertEquals("prometheus", ExporterType.PROMETHEUS.getName());
    }

    @Test
    void getType_invalidTypeName_throwException() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> ExporterType.getType("foo"));
    }

    @Test
    void getType_validTypeNameLowerCase() {
        Assertions.assertEquals(ExporterType.PROMETHEUS, ExporterType.getType("prometheus"));
    }

    @Test
    void getType_validTypeNameMixedCase() {
        Assertions.assertEquals(ExporterType.PROMETHEUS, ExporterType.getType("proMethEus"));
    }
}