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
package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link VersionUtils} 测试类
 *
 * @author wang.liang
 */
class VersionUtilsTest {

    @Test
    void testToLong() {
        System.out.println(Long.MAX_VALUE); // 19位
        System.out.println(VersionUtils.toLong("999")); // 16位

        // case: RELEASE版本，个位数为1
        Assertions.assertEquals(999_000_000_000_000_1L, VersionUtils.toLong("999"));
        Assertions.assertEquals(999_002_000_000_000_1L, VersionUtils.toLong("999.2"));
        Assertions.assertEquals(999_002_000_000_000_1L, VersionUtils.toLong("999_2"));
        Assertions.assertEquals(999_002_003_004_005_1L, VersionUtils.toLong("999.2.3.4.5"));
        Assertions.assertThrows(IncompatibleVersionException.class, () -> VersionUtils.toLong("999.2.3.4.5.6"));
        // case: SNAPSHOT版本，个位数为0
        Assertions.assertEquals(999_000_000_000_000_0L, VersionUtils.toLong("999-SNAPSHOT"));
        Assertions.assertEquals(999_002_000_000_000_0L, VersionUtils.toLong("999.2-SNAPSHOT"));
        Assertions.assertEquals(999_002_000_000_000_0L, VersionUtils.toLong("999_2-SNAPSHOT"));
        Assertions.assertEquals(999_002_003_004_005_0L, VersionUtils.toLong("999.2.3.4.5-SNAPSHOT"));
        Assertions.assertThrows(IncompatibleVersionException.class, () -> VersionUtils.toLong("999.2.3.4.5.6-SNAPSHOT"));
        // case: 0
        Assertions.assertEquals(0L, VersionUtils.toLong(null));
        Assertions.assertEquals(0L, VersionUtils.toLong(""));
        Assertions.assertEquals(0L, VersionUtils.toLong("   "));
        Assertions.assertEquals(0L, VersionUtils.toLong("unknown"));
    }
}
