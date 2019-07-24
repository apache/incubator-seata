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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数器，从0开始，保证正数。
 *
 * @author Geng Zhang
 */
public class PositiveAtomicCounter {
    private static final int    MASK = 0x7FFFFFFF;
    private final AtomicInteger atom;

    public PositiveAtomicCounter() {
        atom = new AtomicInteger(0);
    }

    public final int incrementAndGet() {
        return atom.incrementAndGet() & MASK;
    }

    public final int getAndIncrement() {
        return atom.getAndIncrement() & MASK;
    }

    public int get() {
        return atom.get() & MASK;
    }

}