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
        Assertions.assertEquals(-1L, DurationUtil.parse("").getSeconds());
        Assertions.assertEquals(0L, DurationUtil.parse("8").getSeconds());
        Assertions.assertEquals(8L, DurationUtil.parse("8").toMillis());
        Assertions.assertEquals(0L, DurationUtil.parse("8ms").getSeconds());
        Assertions.assertEquals(8L, DurationUtil.parse("8ms").toMillis());
        Assertions.assertEquals(8L, DurationUtil.parse("8s").getSeconds());
        Assertions.assertEquals(480L, DurationUtil.parse("8m").getSeconds());
        Assertions.assertEquals(28800L, DurationUtil.parse("8h").getSeconds());
        Assertions.assertEquals(691200L, DurationUtil.parse("8d").getSeconds());

        Assertions.assertEquals(172800L,DurationUtil.parse("P2D").getSeconds());
        Assertions.assertEquals(20L,DurationUtil.parse("PT20.345S").getSeconds());
        Assertions.assertEquals(20345L,DurationUtil.parse("PT20.345S").toMillis());
        Assertions.assertEquals(900L,DurationUtil.parse("PT15M").getSeconds());
        Assertions.assertEquals(36000L,DurationUtil.parse("PT10H").getSeconds());
        Assertions.assertEquals(8L,DurationUtil.parse("PT8S").getSeconds());
        Assertions.assertEquals(86460L,DurationUtil.parse("P1DT1M").getSeconds());
        Assertions.assertEquals(183840L,DurationUtil.parse("P2DT3H4M").getSeconds());
        Assertions.assertEquals(-21420L,DurationUtil.parse("PT-6H3M").getSeconds());
        Assertions.assertEquals(-21780L,DurationUtil.parse("-PT6H3M").getSeconds());
        Assertions.assertEquals(21420L,DurationUtil.parse("-PT-6H+3M").getSeconds());
    }

    @Test
    public void testParseThrowException() {
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("a"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("as"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("d"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("h"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("m"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("s"));

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse("ms"));
    }
}
