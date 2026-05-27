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
package org.apache.seata.benchmark.saga;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thread-local failure random provider to avoid cross-thread contention while keeping seeded behavior reproducible.
 */
public final class FailureRandomProvider {

    private FailureRandomProvider() {}

    public static ThreadLocal<Random> create(Long seed) {
        if (seed == null) {
            return null;
        }
        return ThreadLocal.withInitial(
                () -> new Random(mixSeed(seed, Thread.currentThread().getName())));
    }

    public static int nextPercent(ThreadLocal<Random> failureRandom) {
        if (failureRandom != null) {
            return failureRandom.get().nextInt(100);
        }
        return ThreadLocalRandom.current().nextInt(100);
    }

    private static long mixSeed(long seed, String threadName) {
        long mixed = seed ^ threadName.hashCode();
        mixed ^= (mixed << 21);
        mixed ^= (mixed >>> 35);
        mixed ^= (mixed << 4);
        return mixed;
    }
}
