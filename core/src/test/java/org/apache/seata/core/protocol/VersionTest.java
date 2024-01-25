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
package org.apache.seata.core.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test {@link Version}.
 *
 */
public class VersionTest {

    @Test
    public void isAboveOrEqualVersion150() {
        Assertions.assertTrue(Version.isAboveOrEqualVersion150("2.0.2"));
        Assertions.assertTrue(Version.isAboveOrEqualVersion150("1.5"));
        Assertions.assertFalse(Version.isAboveOrEqualVersion150("1.4.9"));
        Assertions.assertFalse(Version.isAboveOrEqualVersion150("")); // Invalid version code will always return false.
        Assertions.assertFalse(Version.isAboveOrEqualVersion150("abd"));
    }

    @Test
    public void testConvertVersion() {
        // case: success
        Assertions.assertDoesNotThrow(() -> {
            long v = Version.convertVersion(Version.getCurrent());
            Assertions.assertTrue(v > 0);
        });
        Assertions.assertDoesNotThrow(() -> {
            long v = Version.convertVersion("1.7.0-SNAPSHOT");
            Assertions.assertEquals(1070000, v);
        });
        Assertions.assertDoesNotThrow(() -> {
            long v = Version.convertVersion("1.7.0");
            Assertions.assertEquals(1070000, v);
        });
        Assertions.assertDoesNotThrow(() -> {
            long v = Version.convertVersion("1.7.0-native-rc1-SNAPSHOT");
            Assertions.assertEquals(1070000, v);
        });
        Assertions.assertDoesNotThrow(() -> {
            long v = Version.convertVersion("1.7.0-native-rc1");
            Assertions.assertEquals(1070000, v);
        });

        // case: fail
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Version.convertVersion(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Version.convertVersion("     ");
        });
        Assertions.assertThrows(IncompatibleVersionException.class, () -> {
            Version.convertVersion("1.7.0.native.rc1-SNAPSHOT");
        });
        Assertions.assertThrows(IncompatibleVersionException.class, () -> {
            Version.convertVersion("1.7.0.native.rc1");
        });
    }
}
