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
package org.apache.seata.server.ratelimiter;

import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.limit.ratelimit.RateLimiter;
import org.apache.seata.server.limit.ratelimit.TokenBucketLimiter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TokenBucketLimiterTest
 */
@SpringBootTest
public class TokenBucketLimiterTest {
    
    /**
     * Logger for TokenBucketLimiterTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketLimiterTest.class);

    @Test
    public void testPerformanceOfTokenBucketLimiter() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketLimiter(true, 1,
                10, 10);
        int threads = 10;
        final int count = 100;
        final CountDownLatch cnt = new CountDownLatch(count * threads);

        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test1", false));
        AtomicInteger totalPass = new AtomicInteger();
        AtomicInteger totalReject = new AtomicInteger();
        StopWatch totalStopWatch = new StopWatch();
        totalStopWatch.start();
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                for (int u = 0; u < count; u++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                        totalPass.getAndIncrement();
                    } else {
                        reject++;
                        totalReject.getAndIncrement();
                    }
                    cnt.countDown();
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
        cnt.await();
        totalStopWatch.stop();
        LOGGER.info("total time:{}ms, total pass:{}, total reject:{}", totalStopWatch.getLastTaskTimeMillis(),
                totalPass.get(), totalReject.get());
        Assertions.assertNotEquals(0, totalReject.get());
    }
}
