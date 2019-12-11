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

public class DurationUtilTest {

    @Test
    public void testParse() {
        Assertions.assertNull(DurationUtil.parse("d"));
        Assertions.assertNull(DurationUtil.parse("h"));
        Assertions.assertNull(DurationUtil.parse("m"));
        Assertions.assertNull(DurationUtil.parse("s"));
        Assertions.assertNull(DurationUtil.parse("ms"));

        Assertions.assertEquals(-1L, DurationUtil.parse("").getSeconds());
        Assertions.assertEquals(0L, DurationUtil.parse("8").getSeconds());
        Assertions.assertEquals(0L, DurationUtil.parse("8ms").getSeconds());
        Assertions.assertEquals(8L, DurationUtil.parse("8s").getSeconds());
        Assertions.assertEquals(480L, DurationUtil.parse("8m").getSeconds());
        Assertions.assertEquals(28800L, DurationUtil.parse("8h").getSeconds());
        Assertions.assertEquals(691200L,
                DurationUtil.parse("8d").getSeconds());
    }

    @Test
    public void testParseThrowException() {
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("a"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("as"));

    }
}
