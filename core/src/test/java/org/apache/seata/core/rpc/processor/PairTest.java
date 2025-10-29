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
package org.apache.seata.core.rpc.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PairTest {

    @Test
    public void testPairCreation() {
        Pair<String, Integer> pair = new Pair<>("test", 123);
        Assertions.assertNotNull(pair);
    }

    @Test
    public void testGetFirst() {
        Pair<String, Integer> pair = new Pair<>("hello", 456);
        Assertions.assertEquals("hello", pair.getFirst());
    }

    @Test
    public void testGetSecond() {
        Pair<String, Integer> pair = new Pair<>("world", 789);
        Assertions.assertEquals(789, pair.getSecond());
    }

    @Test
    public void testPairWithNullValues() {
        Pair<String, Integer> pair = new Pair<>(null, null);
        Assertions.assertNull(pair.getFirst());
        Assertions.assertNull(pair.getSecond());
    }

    @Test
    public void testPairWithDifferentTypes() {
        Pair<Integer, String> intStringPair = new Pair<>(100, "value");
        Assertions.assertEquals(100, intStringPair.getFirst());
        Assertions.assertEquals("value", intStringPair.getSecond());

        Pair<Boolean, Double> boolDoublePair = new Pair<>(true, 3.14);
        Assertions.assertEquals(true, boolDoublePair.getFirst());
        Assertions.assertEquals(3.14, boolDoublePair.getSecond());
    }

    @Test
    public void testPairWithSameTypes() {
        Pair<String, String> stringPair = new Pair<>("first", "second");
        Assertions.assertEquals("first", stringPair.getFirst());
        Assertions.assertEquals("second", stringPair.getSecond());

        Pair<Integer, Integer> intPair = new Pair<>(1, 2);
        Assertions.assertEquals(1, intPair.getFirst());
        Assertions.assertEquals(2, intPair.getSecond());
    }

    @Test
    public void testPairWithComplexObjects() {
        Pair<Pair<String, Integer>, Pair<Boolean, Double>> nestedPair =
                new Pair<>(new Pair<>("nested", 100), new Pair<>(false, 2.71));

        Assertions.assertEquals("nested", nestedPair.getFirst().getFirst());
        Assertions.assertEquals(100, nestedPair.getFirst().getSecond());
        Assertions.assertEquals(false, nestedPair.getSecond().getFirst());
        Assertions.assertEquals(2.71, nestedPair.getSecond().getSecond());
    }

    @Test
    public void testMultiplePairsIndependence() {
        Pair<String, Integer> pair1 = new Pair<>("first", 1);
        Pair<String, Integer> pair2 = new Pair<>("second", 2);

        Assertions.assertEquals("first", pair1.getFirst());
        Assertions.assertEquals(1, pair1.getSecond());
        Assertions.assertEquals("second", pair2.getFirst());
        Assertions.assertEquals(2, pair2.getSecond());
    }
}
