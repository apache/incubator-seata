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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class DurationUtilTest {

    private static Stream<Arguments> provideValueSetsTestParseGetSeconds() {
        return Stream.of(
                Arguments.of(-1L, ""),
                Arguments.of(0L, "8"),
                Arguments.of(0L, "8ms"),
                Arguments.of(8L, "8s"),
                Arguments.of(480L, "8m"),
                Arguments.of(28800L, "8h"),
                Arguments.of(691200L, "8d"),
                Arguments.of(172800L, "P2D"),
                Arguments.of(20L, "PT20.345S"),
                Arguments.of(900L, "PT15M"),
                Arguments.of(36000L, "PT10H"),
                Arguments.of(8L, "PT8S"),
                Arguments.of(86460L, "P1DT1M"),
                Arguments.of(183840L, "P2DT3H4M"),
                Arguments.of(-21420L, "PT-6H3M"),
                Arguments.of(-21780L, "-PT6H3M"),
                Arguments.of(21420L, "-PT-6H+3M")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValueSetsTestParseGetSeconds")
    public void testParseGetSeconds(long expected, String str) {
        Assertions.assertEquals(expected,DurationUtil.parse(str).getSeconds());
    }

    private static Stream<Arguments> provideValueSetsTestParseToMillis() {
        return Stream.of(
                Arguments.of(8L, "8"),
                Arguments.of(8L, "8ms"),
                Arguments.of(20345L, "PT20.345S")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValueSetsTestParseToMillis")
    public void testParseToMillis(long expected, String str) {
        Assertions.assertEquals(expected, DurationUtil.parse(str).toMillis());
    }

    private static Stream<Arguments> provideValueSetsTestParseThrowException() {
        return Stream.of(
                Arguments.of("a"),
                Arguments.of("as"),
                Arguments.of("d"),
                Arguments.of("h"),
                Arguments.of("m"),
                Arguments.of("s"),
                Arguments.of("ms")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValueSetsTestParseThrowException")
    public void testParseThrowException(String str) {
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> DurationUtil.parse(str));
    }
}
