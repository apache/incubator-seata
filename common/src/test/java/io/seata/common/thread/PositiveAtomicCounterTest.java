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
package io.seata.common.thread;

import org.junit.jupiter.api.Test;

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
}
