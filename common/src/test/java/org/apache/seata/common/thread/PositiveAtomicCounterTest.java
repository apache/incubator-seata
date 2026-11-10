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
package org.apache.seata.common.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PositiveAtomicCounterTest {

    @Test
    public void testConstructor() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter).isInstanceOf(PositiveAtomicCounter.class);
    }

    @Test
    public void testIncrementAndGet() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.incrementAndGet()).isEqualTo(1);
    }

    @Test
    public void testGetAndIncrement() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.getAndIncrement()).isEqualTo(0);
    }

    @Test
    public void testGet() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        assertThat(counter.get()).isEqualTo(0);
    }

    @Test
    public void testMultipleOperations() {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();

        // Test sequence of operations
        assertThat(counter.getAndIncrement()).isEqualTo(0); // Returns 0, then increments
        assertThat(counter.get()).isEqualTo(1); // Current value is 1
        assertThat(counter.incrementAndGet()).isEqualTo(2); // Increments then returns 2
        assertThat(counter.get()).isEqualTo(2); // Current value is 2
        assertThat(counter.getAndIncrement()).isEqualTo(2); // Returns 2, then increments
        assertThat(counter.get()).isEqualTo(3); // Current value is 3
    }

    @Test
    public void testIncrementAndGetWrapsAtIntegerMaxValue() throws ReflectiveOperationException {
        PositiveAtomicCounter counter = counterWithValue(Integer.MAX_VALUE - 1);

        assertThat(counter.incrementAndGet()).isEqualTo(Integer.MAX_VALUE);
        assertThat(counter.incrementAndGet()).isZero();
        assertThat(counter.get()).isZero();
        assertThat(counter.incrementAndGet()).isEqualTo(1);
    }

    @Test
    public void testGetAndIncrementWrapsAtIntegerMaxValue() throws ReflectiveOperationException {
        PositiveAtomicCounter counter = counterWithValue(Integer.MAX_VALUE);

        assertThat(counter.getAndIncrement()).isEqualTo(Integer.MAX_VALUE);
        assertThat(counter.get()).isZero();
        assertThat(counter.getAndIncrement()).isZero();
        assertThat(counter.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"-2147483648, 0", "-2147483647, 1", "-1, 2147483647"})
    public void testGetReturnsNonNegativeValueWithoutIncrementing(int internalValue, int expectedValue)
            throws ReflectiveOperationException {
        PositiveAtomicCounter counter = counterWithValue(internalValue);

        assertThat(counter.get()).isEqualTo(expectedValue);
        assertThat(counter.get()).isEqualTo(expectedValue);
    }

    private PositiveAtomicCounter counterWithValue(int value) throws ReflectiveOperationException {
        PositiveAtomicCounter counter = new PositiveAtomicCounter();
        Field atomField = PositiveAtomicCounter.class.getDeclaredField("atom");
        atomField.setAccessible(true);
        ((AtomicInteger) atomField.get(counter)).set(value);
        return counter;
    }
}
