/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.common.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by guoyao on 2019/2/26.
 */
public class RejectedPoliciesTest {

    private final int DEFAULT_CORE_POOL_SIZE = 1;
    private final int DEFAULT_KEEP_ALIVE_TIME = 10;
    private final int MAX_QUEUE_SIZE = 1;

    /**
     * Test runs oldest task policy.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRunsOldestTaskPolicy() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger();
        ThreadPoolExecutor poolExecutor =
            new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_CORE_POOL_SIZE, DEFAULT_KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue(MAX_QUEUE_SIZE),
                new NamedThreadFactory("OldestRunsPolicy", DEFAULT_CORE_POOL_SIZE),
                RejectedPolicies.runsOldestTaskPolicy());
        CountDownLatch downLatch1 = new CountDownLatch(1);
        CountDownLatch downLatch2 = new CountDownLatch(1);
        CountDownLatch downLatch3 = new CountDownLatch(1);
        //task1
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //wait the oldest task of queue count down
                    downLatch1.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                atomicInteger.getAndAdd(1);
            }
        });
        assertThat(atomicInteger.get()).isEqualTo(0);
        //task2
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // run second
                atomicInteger.getAndAdd(2);
            }
        });
        //task3
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                downLatch2.countDown();
                //task3 run
                atomicInteger.getAndAdd(3);
                downLatch3.countDown();
            }
        });
        //only the task2 run which is the oldest task of queue
        assertThat(atomicInteger.get()).isEqualTo(2);
        downLatch1.countDown();
        downLatch2.await();
        //wait task3 run +3
        downLatch3.await();
        //run task3
        assertThat(atomicInteger.get()).isEqualTo(6);

    }
}
